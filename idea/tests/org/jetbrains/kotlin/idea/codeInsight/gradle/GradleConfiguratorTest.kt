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

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinStatus
import org.jetbrains.kotlin.idea.configuration.KotlinGradleModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.junit.Test

class GradleConfiguratorTest : GradleImportingTestCase() {
    @Test
    fun testProjectWithModule() {
        createProjectSubFile("settings.gradle", "include ':app'")
        createProjectSubFile("app/build.gradle", """
        buildscript {
            repositories {
                jcenter()
                mavenCentral()
            }
            dependencies {
                classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0"
            }
        }

        apply plugin: 'kotlin'

        dependencies {
            compile "org.jetbrains.kotlin:kotlin-stdlib-0.0"   // intentionally invalid version
        }
        """.trimIndent())

        importProject()

        runInEdtAndWait {
            runWriteAction {
                // Create not configured build.gradle for project
                myProject.baseDir.createChildData(null, "build.gradle")

                val module = ModuleManager.getInstance(myProject).findModuleByName("app")!!
                val configurator = Extensions.findExtension(KotlinProjectConfigurator.EP_NAME,
                                                            KotlinGradleModuleConfigurator::class.java)
                // We have a Kotlin runtime in build.gradle but not in the classpath, so it doesn't make sense
                // to suggest configuring it
                assertEquals(ConfigureKotlinStatus.BROKEN, configurator.getStatus(module))
            }
        }
    }
}
