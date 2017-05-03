/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.inspections.gradle.DifferentKotlinGradleVersionInspection
import org.jetbrains.kotlin.idea.inspections.gradle.DifferentStdlibGradleVersionInspection
import org.jetbrains.kotlin.idea.inspections.runInspection
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.junit.Assert
import org.junit.Test

class GradleInspectionTest : GradleImportingTestCase() {
    @Test
    fun testDifferentStdlibGradleVersion() {
        val localFile = createProjectSubFile("build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.0.2")
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.0.3"
            }
        """)
        importProject()

        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResult(tool, localFile)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals("Plugin version (1.0.2) is not the same as library version (1.0.3)", problems.single())
    }

    @Test
    fun testDifferentStdlibJre7GradleVersion() {
        val localFile = createProjectSubFile("build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    mavenCentral()
                    maven {
                        url 'http://dl.bintray.com/kotlin/kotlin-eap-1.1'
                    }
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0-beta-17")
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib-jre7:1.1.0-beta-22"
            }
        """)
        importProject()

        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResult(tool, localFile)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals("Plugin version (1.1.0-beta-17) is not the same as library version (1.1.0-beta-22)", problems.single())
    }

    @Test
    fun testDifferentStdlibGradleVersionWithVariables() {
        createProjectSubFile("gradle.properties", """
        |kotlin=1.0.1
        |lib_version=1.0.3""".trimMargin())
        val localFile = createProjectSubFile("build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin")
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile group: 'org.jetbrains.kotlin', name: 'kotlin-stdlib', version: lib_version
            }
        """)
        importProject()

        val tool = DifferentStdlibGradleVersionInspection()
        val problems = getInspectionResult(tool, localFile)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals("Plugin version (1.0.1) is not the same as library version (1.0.3)", problems.single())
    }

    @Test
    fun testDifferentKotlinGradleVersion() {
        createProjectSubFile("gradle.properties", """test=1.0.1""")
        val localFile = createProjectSubFile("build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{test}")
                }
            }

            apply plugin: 'kotlin'
        """)
        importProject()

        val tool = DifferentKotlinGradleVersionInspection()
        tool.testVersionMessage = "\$PLUGIN_VERSION"
        val problems = getInspectionResult(tool, localFile)

        Assert.assertTrue(problems.size == 1)
        Assert.assertEquals("Kotlin version that is used for building with Gradle (1.0.1) differs from the one bundled into the IDE plugin (\$PLUGIN_VERSION)", problems.single())
    }

    fun getInspectionResult(tool: LocalInspectionTool, file: VirtualFile): List<String> {
        val resultRef = Ref<List<String>>()
        invokeTestRunnable {
            val presentation = runInspection(tool, myProject, listOf(file))

            val foundProblems = presentation.problemElements
                    .values
                    .flatMap { it.toList() }
                    .mapNotNull { it as? ProblemDescriptorBase }
                    .map { it.descriptionTemplate }

            resultRef.set(foundProblems)
        }

        return resultRef.get()
    }


    override fun invokeTestRunnable(runnable: Runnable) {
        runInEdtAndWait {
            runnable.run()
        }
    }
}