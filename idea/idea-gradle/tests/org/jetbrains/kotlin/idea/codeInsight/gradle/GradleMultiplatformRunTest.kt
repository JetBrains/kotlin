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

import com.intellij.compiler.options.CompileStepBeforeRun
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.psi.PsiManager
import com.intellij.testFramework.MapDataContext
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Test

class GradleMultiplatformRunTest : GradleImportingTestCase() {
    @Test
    fun testMultiplatformClasspath() {
        createProjectSubFile(
                "build.gradle",
                """
                buildscript {
                    repositories {
                        jcenter()
                        mavenCentral()
                    }
                    dependencies {
                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.4")
                    }
                }

                apply plugin: 'kotlin-platform-common'

                repositories {
                    jcenter()
                    mavenCentral()
                }

                dependencies {
                    compile "org.jetbrains.kotlin:kotlin-stdlib-common:1.1.4"
                }

                """.trimIndent()
        )
        createProjectSubFile(
                "settings.gradle",
                """
                    rootProject.name = 'MultiTest'
                    include 'MultiTest-jvm', 'MultiTest-js'
                """.trimIndent()
        )
        createProjectSubFile(
                "MultiTest-js/build.gradle",
                """
                buildscript {
                    repositories {
                        jcenter()
                        mavenCentral()
                    }
                    dependencies {
                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.4")
                    }
                }

                apply plugin: 'kotlin-platform-js'

                repositories {
                        jcenter()
                        mavenCentral()
                    }

                dependencies {
                    compile "org.jetbrains.kotlin:kotlin-stdlib-js:1.1.4"
                    implement project(":")
                }

                """.trimIndent()
        )
        createProjectSubFile(
                "MultiTest-jvm/build.gradle",
                """
                buildscript {
                    repositories {
                        jcenter()
                        mavenCentral()
                    }
                    dependencies {
                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.4")
                    }
                }

                apply plugin: 'kotlin-platform-jvm'

                repositories {
                    jcenter()
                    mavenCentral()
                }

                dependencies {
                    compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.4"
                    implement project(":")
                }

                """.trimIndent()
        )
        val virtualFile = createProjectSubFile("src/main/kotlin/foo.kt", "fun main(args: Array<String>) { println(\"Foo!\") }")

        importProject()

        val javaParameters = invokeAndWaitIfNeed {
            val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile) as KtFile
            val function = psiFile.declarations.single()
            val dataContext = MapDataContext().apply {
                put(CommonDataKeys.PROJECT, myProject)
                put(LangDataKeys.MODULE, function.module)
                put(Location.DATA_KEY, PsiLocation.fromPsiElement(function))
            }
            val configurationContext = ConfigurationContext.getFromContext(dataContext)
            val producer = RunConfigurationProducer.getInstance(KotlinRunConfigurationProducer::class.java)
            val configuration = producer.createConfigurationFromContext(configurationContext)!!

            val executionEnvironment = ExecutionEnvironmentBuilder.create(myProject,
                                                                          DefaultRunExecutor.getRunExecutorInstance(),
                                                                          configuration.configuration)
                .build()

            val compileStepBeforeRun = CompileStepBeforeRun(myProject)
            compileStepBeforeRun.executeTask(dataContext, configuration.configuration, executionEnvironment,
                                             CompileStepBeforeRun.MakeBeforeRunTask())

            val state = configuration.configuration.getState(DefaultRunExecutor.getRunExecutorInstance(), executionEnvironment) as JavaCommandLineState
            state.javaParameters

        }

        val p = javaParameters.classPath
        assertTrue(p.virtualFiles.any { it.findChild("FooKt.class") != null })
    }
}