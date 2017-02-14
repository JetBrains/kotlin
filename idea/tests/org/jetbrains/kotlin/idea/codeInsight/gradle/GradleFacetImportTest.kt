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

import org.jetbrains.kotlin.config.CoroutineSupport
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.junit.Assert
import org.junit.Test

class GradleFacetImportTest : GradleImportingTestCase() {
    private val facetSettings: KotlinFacetSettings
        get() = KotlinFacet.get(getModule("project_main"))!!.configuration.settings

    // TODO: Update this test to 1.1-RC when it's available
    @Test
    fun testJvmImport() {
        createProjectSubFile("build.gradle", """
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
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0-beta-38")
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.0-beta-38"
            }

            compileKotlin {
                kotlinOptions.jvmTarget = "1.7"
                kotlinOptions.freeCompilerArgs = ["-Xsingle-module", "-Xdump-declarations-to", "tmp"]
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", versionInfo.languageLevel!!.versionString)
            Assert.assertEquals("1.1", versionInfo.apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.Jvm[JvmTarget.JVM_1_8], versionInfo.targetPlatformKind)
            Assert.assertEquals("1.7", compilerInfo.k2jvmCompilerArguments!!.jvmTarget)
            Assert.assertEquals("-no-stdlib -no-reflect -module-name project_main -Xdump-declarations-to tmp -Xsingle-module -Xadd-compiler-builtins",
                                compilerInfo.compilerSettings!!.additionalArguments)
        }
    }

    @Test
    fun testCoroutineImportByOptions() {
        createProjectSubFile("build.gradle", """
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
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0-beta-38")
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.0-beta-38"
            }

            kotlin {
                experimental {
                    coroutines 'enable'
                }
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals(CoroutineSupport.ENABLED, compilerInfo.coroutineSupport)
        }
    }

    @Test
    fun testFixCorruptedCoroutines() {
        createProjectSubFile("build.gradle", """
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
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0-beta-38")
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.0-beta-38"
            }

            kotlin {
                experimental {
                    coroutines 'enable'
                }
            }
        """)

        importProject()

        with (facetSettings) {
            compilerInfo.k2jvmCompilerArguments!!.coroutinesEnable = true
            compilerInfo.k2jvmCompilerArguments!!.coroutinesWarn = true
            compilerInfo.k2jvmCompilerArguments!!.coroutinesError = true

            importProject()

            Assert.assertEquals(CoroutineSupport.ENABLED, compilerInfo.coroutineSupport)
            Assert.assertEquals(true, compilerInfo.k2jvmCompilerArguments!!.coroutinesEnable)
            Assert.assertEquals(false, compilerInfo.k2jvmCompilerArguments!!.coroutinesWarn)
            Assert.assertEquals(false, compilerInfo.k2jvmCompilerArguments!!.coroutinesError)
        }
    }

    @Test
    fun testCoroutineImportByProperties() {
        createProjectSubFile("gradle.properties", "kotlin.coroutines=enable")
        createProjectSubFile("build.gradle", """
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
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0-beta-38")
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.0-beta-38"
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals(CoroutineSupport.ENABLED, compilerInfo.coroutineSupport)
        }
    }

    // TODO: Uncomment the test below when 1.1-RC is available (see KT-16174)
    /*@Test
    fun testJsImport() {
        createProjectSubFile("build.gradle", """
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
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0-rc")
                }
            }

            apply plugin: 'kotlin2js'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib-js:1.1.0-rc"
            }

            compileKotlin2Js {
                kotlinOptions.sourceMap = true
                kotlinOptions.freeCompilerArgs = ["-module-kind", "plain"]
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", versionInfo.languageLevel!!.versionString)
            Assert.assertEquals("1.1", versionInfo.apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.JavaScript, versionInfo.targetPlatformKind)
            Assert.assertEquals(true, compilerInfo.k2jsCompilerArguments!!.sourceMap)
            Assert.assertEquals("-source-map -module-kind plain -target v5 -main call",
                                compilerInfo.compilerSettings!!.additionalArguments)
        }
    }*/

    @Test
    fun testDetectOldJsStdlib() {
        createProjectSubFile("build.gradle", """
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
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.0.6")
                }
            }

            apply plugin: 'kotlin2js'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-js-library:1.0.6"
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals(TargetPlatformKind.JavaScript, versionInfo.targetPlatformKind)
        }
    }

    @Test
    fun testCommonImport() {
        createProjectSubFile("build.gradle", """
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
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0-beta-38")
                }
            }

            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib-common:1.1.0-beta-38"
            }
        """)
        importProject()

        with (facetSettings) {
            Assert.assertEquals("1.1", versionInfo.languageLevel!!.versionString)
            Assert.assertEquals("1.1", versionInfo.apiLevel!!.versionString)
            Assert.assertEquals(TargetPlatformKind.Common, versionInfo.targetPlatformKind)
        }
    }
}
