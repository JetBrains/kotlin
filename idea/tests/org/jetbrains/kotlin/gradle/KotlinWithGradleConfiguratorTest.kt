/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile

class KotlinWithGradleConfiguratorTest : LightCodeInsightFixtureTestCase() {
    fun testAddCoroutinesSupport() {
        val buildGradle = myFixture.configureByText("build.gradle", "apply plugin: 'kotlin'\n") as GroovyFile
        myFixture.project.executeWriteCommand("") {
            KotlinWithGradleConfigurator.changeCoroutineConfiguration(buildGradle, "enable")
        }
        myFixture.checkResult(
                """apply plugin: 'kotlin'
                  |kotlin {
                  |    experimental {
                  |        coroutines "enable"
                  |    }
                  |}
                  |""".trimMargin("|"))
    }

    fun testChangeCoroutinesSupport() {
        val buildGradle = myFixture.configureByText("build.gradle",
               """apply plugin: 'kotlin'
                  |kotlin {
                  |    experimental {
                  |        coroutines "disable"
                  |    }
                  |}
                  |""".trimMargin("|")) as GroovyFile
        myFixture.project.executeWriteCommand("") {
            KotlinWithGradleConfigurator.changeCoroutineConfiguration(buildGradle, "enable")
        }
        myFixture.checkResult(
                """apply plugin: 'kotlin'
                  |kotlin {
                  |    experimental {
                  |        coroutines "enable"
                  |    }
                  |}
                  |""".trimMargin("|"))
    }

    fun testAddLanguageVersion() {
        val buildGradle = myFixture.configureByText("build.gradle", "apply plugin: 'kotlin'\n") as GroovyFile
        myFixture.project.executeWriteCommand("") {
            KotlinWithGradleConfigurator.changeLanguageVersion(buildGradle, "1.1", false)
        }
        myFixture.checkResult(
                """apply plugin: 'kotlin'
                  |compileKotlin {
                  |    kotlinOptions {
                  |        languageVersion = "1.1"
                  |    }
                  |}
                  |""".trimMargin("|"))
    }

    fun testChangeLanguageVersion() {
        val buildGradle = myFixture.configureByText("build.gradle",
               """apply plugin: 'kotlin'
                  |compileKotlin {
                  |    kotlinOptions {
                  |        languageVersion = "1.0"
                  |    }
                  |}
                  |""".trimMargin("|")) as GroovyFile
        myFixture.project.executeWriteCommand("") {
            KotlinWithGradleConfigurator.changeLanguageVersion(buildGradle, "1.1", false)
        }
        myFixture.checkResult(
                """apply plugin: 'kotlin'
                  |compileKotlin {
                  |    kotlinOptions {
                  |        languageVersion = "1.1"
                  |    }
                  |}
                  |""".trimMargin("|"))
    }
}
