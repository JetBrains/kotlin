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

import com.intellij.openapi.roots.DependencyScope
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.junit.Test

class MultiplatformProjectImportingTest : GradleImportingTestCase() {
    @Test
    fun testPlatformToCommonDependency() {
        createProjectSubFile("settings.gradle", "include ':common', ':jvm', ':js'")

        val kotlinVersion = "1.1.0"

        createProjectSubFile("build.gradle", """
             buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                }
            }

            project('common') {
                apply plugin: 'kotlin-platform-common'
            }

            project('jvm') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    implement project(':common')
                }
            }

            project('js') {
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    implement project(':common')
                }
            }
        """)

        importProject()
        assertModuleModuleDepScope("jvm_main", "common_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm_test", "common_test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_main", "common_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js_test", "common_test", DependencyScope.COMPILE)
    }

    @Test
    fun testMultiProject() {
        createProjectSubFile("settings.gradle", "include ':common-lib', ':jvm-lib', ':js-lib', ':common-app', ':jvm-app', ':js-app'")

        val kotlinVersion = "1.1.0"

        createProjectSubFile("build.gradle", """
             buildscript {
                repositories {
                    mavenCentral()
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                }
            }

            project('common-lib') {
                apply plugin: 'kotlin-platform-common'
            }

            project('jvm-lib') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    implement project(':common-lib')
                }
            }

            project('js-lib') {
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    implement project(':common-lib')
                }
            }

            project('common-app') {
                apply plugin: 'kotlin-platform-common'

                dependencies {
                    compile project(':common-lib')
                }
            }

            project('jvm-app') {
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    implement project(':common-app')
                    compile project(':jvm-lib')
                }
            }

            project('js-app') {
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    implement project(':common-app')
                    compile project(':js-lib')
                }
            }
        """)

        importProject()

        assertModuleModuleDepScope("jvm-app_main", "common-app_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_main", "common-lib_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("jvm-app_main", "jvm-lib_main", DependencyScope.COMPILE)

        assertModuleModuleDepScope("js-app_main", "common-app_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js-app_main", "common-lib_main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("js-app_main", "js-lib_main", DependencyScope.COMPILE)
    }
}