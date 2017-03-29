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

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.junit.Assert
import org.junit.Test
import java.io.File

class GradleFacetImportTest_3_3 : GradleImportingTestCase() {
    override fun setUp() {
        gradleVersion = "3.3"
        super.setUp()
    }

    @Test
    fun testAndroidGradleJsDetection() {
        createProjectSubFile("android-module/build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    jcenter()
                }
                dependencies {
                    classpath "com.android.tools.build:gradle:2.3.0-rc1"
                }
            }

            apply plugin: 'com.android.application'

            android {
                compileSdkVersion 23
                buildToolsVersion "23.0.1"

                defaultConfig {
                    minSdkVersion 11
                    targetSdkVersion 23
                    versionCode 1002003
                    versionName version
                }

                dataBinding {
                    enabled = true
                }

                compileOptions {
                    sourceCompatibility JavaVersion.VERSION_1_7
                    targetCompatibility JavaVersion.VERSION_1_7
                }

                buildTypes {
                    debug {
                        applicationIdSuffix ".debug"
                        versionNameSuffix "-debug"
                    }
                    release {
                        minifyEnabled true
                        shrinkResources true
                    }
                }
            }
        """)
        createProjectSubFile("android-module/src/main/AndroidManifest.xml", """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                      xmlns:tools="http://schemas.android.com/tools"
                      package="my.test.project" >
            </manifest>
        """)
        createProjectSubFile("js-module/build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    mavenCentral()
                    maven {
                        url 'http://dl.bintray.com/kotlin/kotlin-dev'
                    }
                }

                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.2-eap-44")
                }
            }

            apply plugin: 'kotlin2js'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib-js:1.1.0"
            }
        """)
        createProjectSubFile("build.gradle", """
            group 'Again'
            version '1.0-SNAPSHOT'

            buildscript {
                repositories {
                    mavenLocal()
                    maven {
                        url='https://dl.bintray.com/kotlin/kotlin-eap-1.1'
                    }
                    jcenter()
                }
                dependencies {
                    classpath "com.android.tools.build:gradle:2.3.0-rc1"
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.0"
                }
            }

            ext {
                androidBuildToolsVersion = '23.0.1'
            }

            allprojects {
                repositories {
                    mavenLocal()
                    maven {
                        url='https://dl.bintray.com/kotlin/kotlin-eap-1.1'
                    }
                    jcenter()
                }
            }
        """)
        createProjectSubFile("settings.gradle", """
            rootProject.name = "android-js-test"
            include ':android-module'
            include ':js-module'
        """)
        createProjectSubFile("local.properties", """
            sdk.dir=/${StringUtil.escapeBackSlashes(File(homePath).parent + "/dependencies/androidSDK")}
        """)
        importProject()

        with (facetSettings("js-module")) {
            Assert.assertEquals(TargetPlatformKind.JavaScript, targetPlatformKind)
        }
    }
}
