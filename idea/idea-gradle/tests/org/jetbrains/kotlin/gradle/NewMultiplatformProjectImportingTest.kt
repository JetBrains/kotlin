/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.roots.*
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinResourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import org.junit.runners.Parameterized

class NewMultiplatformProjectImportingTest : GradleImportingTestCase() {
    private val kotlinVersion = "1.3.0-rc-146"

    @Test
    fun testProjectDependency() {
        createProjectSubFile(
            "settings.gradle",
            "include 'lib', 'app'"
        )
        createProjectSubFile(
            "build.gradle",
            """
                buildscript {
                    repositories {
                        mavenLocal()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
                    }
                }

                allprojects {
                    repositories {
                        mavenLocal()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                }
            """.trimIndent()
        )
        createProjectSubFile(
            "app/build.gradle",
            """
                apply plugin: 'kotlin-multiplatform'

                kotlin {
                    sourceSets {
                        commonMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib-common'
                                implementation project(':lib')
                            }
                        }
                        main {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib'
                            }
                        }
                        jsMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib-js'
                            }
                        }
                    }
                    targets {
                        fromPreset(presets.jvmWithJava, 'jvm')
                        fromPreset(presets.js, 'js')
                    }
                }

                apply plugin: 'application'
                mainClassName = 'com.example.app.JvmGreeterKt'
            """.trimIndent()
        )
        createProjectSubFile(
            "lib/build.gradle",
            """
                apply plugin: 'kotlin-multiplatform'

                kotlin {
                    sourceSets {
                        commonMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib-common'
                            }
                        }
                        jvmMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib'
                            }
                        }
                        jsMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib-js'
                            }
                        }
                    }
                    targets {
                        fromPreset(presets.jvm, 'jvm')
                        fromPreset(presets.js, 'js')
                    }
                }
            """.trimIndent()
        )

        importProject()

        checkProjectStructure() {
            allModules {
                languageVersion("1.3")
                apiVersion("1.3")
                when (module.name) {
                    "project", "app", "lib" -> additionalArguments(null)
                    else -> additionalArguments("-version")
                }
            }

            module("project")
            module("app")
            module("app_commonMain") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/commonMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("app/src/commonMain/resources", KotlinResourceRootType.Resource)
                inheritProjectOutput()
            }
            module("app_commonTest") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("app_commonMain", DependencyScope.TEST)
                sourceFolder("app/src/commonTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("app/src/commonTest/resources", KotlinResourceRootType.TestResource)
                inheritProjectOutput()
            }
            module("app_jsMain") {
                platform(JsIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_jsMain", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/jsMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("app/src/jsMain/resources", KotlinResourceRootType.Resource)
                outputPath("app/build/classes/kotlin/js/main", true)
            }
            module("app_jsTest") {
                platform(JsIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.TEST)
                moduleDependency("lib_jsMain", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("app_commonMain", DependencyScope.TEST)
                moduleDependency("app_commonTest", DependencyScope.TEST)
                moduleDependency("app_jsMain", DependencyScope.TEST)
                sourceFolder("app/src/jsTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("app/src/jsTest/resources", KotlinResourceRootType.TestResource)
                outputPath("app/build/classes/kotlin/js/test", false)
            }
            module("app_jvmMain") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_jvmMain", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                moduleDependency("app_main", DependencyScope.COMPILE)
                moduleDependency("app_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/jvmMain/resources", JavaResourceRootType.RESOURCE)
                outputPath("app/build/classes/kotlin/jvm/main", true)
            }
            module("app_jvmTest") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("lib_jvmMain", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("app_test", DependencyScope.TEST)
                moduleDependency("app_jvmMain", DependencyScope.TEST)
                moduleDependency("app_commonMain", DependencyScope.TEST)
                moduleDependency("app_commonTest", DependencyScope.TEST)
                sourceFolder("app/src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
                outputPath("app/build/classes/kotlin/jvm/test", false)
            }
            module("app_main") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_8))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/main/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/resources", JavaResourceRootType.RESOURCE)
                inheritProjectOutput()
            }
            module("app_test") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_8))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("app_main", DependencyScope.TEST)
                sourceFolder("app/src/test/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/resources", JavaResourceRootType.TEST_RESOURCE)
                inheritProjectOutput()
            }
            module("lib")
            module("lib_commonMain") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                sourceFolder("lib/src/commonMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("lib/src/commonMain/resources", KotlinResourceRootType.Resource)
                inheritProjectOutput()
            }
            module("lib_commonTest") {
                platform(CommonIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                sourceFolder("lib/src/commonTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("lib/src/commonTest/resources", KotlinResourceRootType.TestResource)
                inheritProjectOutput()
            }
            module("lib_jsMain") {
                platform(JsIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jsMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("lib/src/jsMain/resources", KotlinResourceRootType.Resource)
                outputPath("lib/build/classes/kotlin/js/main", true)
            }
            module("lib_jsTest") {
                platform(JsIdePlatformKind.Platform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("lib_commonTest", DependencyScope.TEST)
                moduleDependency("lib_jsMain", DependencyScope.TEST)
                sourceFolder("lib/src/jsTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("lib/src/jsTest/resources", KotlinResourceRootType.TestResource)
                outputPath("lib/build/classes/kotlin/js/test", false)
            }
            module("lib_jvmMain") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("lib_commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("lib/src/jvmMain/resources", JavaResourceRootType.RESOURCE)
                outputPath("lib/build/classes/kotlin/jvm/main", true)
            }
            module("lib_jvmTest") {
                platform(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_6))
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("lib_commonTest", DependencyScope.TEST)
                moduleDependency("lib_commonMain", DependencyScope.TEST)
                moduleDependency("lib_jvmMain", DependencyScope.TEST)
                sourceFolder("lib/src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("lib/src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
                outputPath("lib/build/classes/kotlin/jvm/test", false)
            }
        }
    }

    @Test
    fun testFileCollectionDependency() {
        createProjectSubFile(
            "build.gradle",
            """
                buildscript {
                    repositories {
                        mavenLocal()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
                    }
                }

                allprojects {
                    repositories {
                        mavenLocal()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                }

                apply plugin: 'kotlin-multiplatform'

                kotlin {
                    sourceSets {
                        jvmMain {
                            dependencies {
                                implementation files("a", "b")
                            }
                        }
                    }
                    targets {
                        fromPreset(presets.jvmWithJava, 'jvm')
                    }
                }
            """.trimIndent()
        )

        importProject()

        checkProjectStructure(
            exhaustiveModuleList = false,
            exhaustiveSourceSourceRootList = false
        ) {
            module("project_jvmMain") {
                libraryDependencyByUrl("file://$projectPath/a", DependencyScope.COMPILE)
                libraryDependencyByUrl("file://$projectPath/b", DependencyScope.COMPILE)
                moduleDependency("project_commonMain", DependencyScope.COMPILE)
                moduleDependency("project_main", DependencyScope.COMPILE)
            }
        }
    }

    @Test
    fun testUnresolvedDependency() {
        createProjectSubFile(
            "build.gradle",
            """
                buildscript {
                    repositories {
                        mavenLocal()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
                    }
                }

                allprojects {
                    repositories {
                        mavenLocal()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                }

                apply plugin: 'kotlin-multiplatform'

                kotlin {
                    sourceSets {
                        jvmMain {
                            dependencies {
                                implementation 'my.lib:unresolved'
                            }
                        }
                    }
                    targets {
                        fromPreset(presets.jvmWithJava, 'jvm')
                    }
                }
            """.trimIndent()
        )

        importProject()

        checkProjectStructure(
            exhaustiveSourceSourceRootList = false,
            exhaustiveDependencyList = false
        ) {
            module("project")
            module("project_commonMain")
            module("project_commonTest")
            module("project_jvmMain")
            module("project_jvmTest")
            module("project_main")
            module("project_test")
        }
    }

    @Test
    fun testAndroidDependencyOnMPP() {
        createProjectSubFile(
            "settings.gradle",
            """
                include ':app'
                include ':shared'
            """.trimIndent()
        )
        createProjectSubFile(
            "build.gradle",
            """
                buildscript {
                    ext.kotlin_version = '$kotlinVersion'
                    repositories {
                        google()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                    dependencies {
                        classpath 'com.android.tools.build:gradle:3.2.0'
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version"
                    }
                }

                allprojects {
                    repositories {
                        google()
                        jcenter()
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                }

                task clean(type: Delete) {
                    delete rootProject.buildDir
                }
            """.trimIndent()
        )
        createProjectSubFile(
            "local.properties",
            "sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}"
        )
        createProjectSubFile(
            "shared/build.gradle",
            """
                apply plugin: 'kotlin-multiplatform'

                kotlin {
                    targets {
                        fromPreset(presets.jvm, 'android')

                        fromPreset(presets.iosArm64, 'iOS') {
                            compilations.main.outputKinds('FRAMEWORK')
                        }
                    }

                    sourceSets {
                        commonMain.dependencies {
                            api 'org.jetbrains.kotlin:kotlin-stdlib-common'
                        }

                        androidMain.dependencies {
                            api 'org.jetbrains.kotlin:kotlin-stdlib'
                        }
                    }
                }

                configurations {
                    compileClasspath
                }
            """.trimIndent()
        )

        createProjectSubFile(
            "app/build.gradle",
            """
                apply plugin: 'com.android.application'

                apply plugin: 'kotlin-android'

                apply plugin: 'kotlin-android-extensions'

                android {
                    compileSdkVersion 26
                    defaultConfig {
                        applicationId "jetbrains.org.myapplication"
                        minSdkVersion 15
                        targetSdkVersion 26
                        versionCode 1
                        versionName "1.0"
                        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
                    }
                    buildTypes {
                        release {
                            minifyEnabled false
                            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
                        }
                    }
                }

                dependencies {
                    implementation project(':shared')
                    implementation fileTree(dir: 'libs', include: ['*.jar'])
                    implementation"org.jetbrains.kotlin:kotlin-stdlib-jdk7:${'$'}kotlin_version"
                    implementation 'com.android.support:appcompat-v7:27.1.1'
                    implementation 'com.android.support.constraint:constraint-layout:1.1.3'
                    testImplementation 'junit:junit:4.12'
                    androidTestImplementation 'com.android.support.test:runner:1.0.2'
                    androidTestImplementation 'com.android.support.test.espresso:espresso-core:3.0.2'
                }
            """.trimIndent()
        )

        importProject()

        checkProjectStructure {
            module("project")
            module("app") {
                libraryDependency("Gradle: android-android-26", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.core:common:1.1.0@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.core:runtime-1.1.0", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:common:1.1.0@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:livedata-core-1.1.0", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:runtime-1.1.0", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:viewmodel-1.1.0", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support.constraint:constraint-layout-1.1.3", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support.constraint:constraint-layout-solver:1.1.3@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support.test.espresso:espresso-core-3.0.2", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support.test.espresso:espresso-idling-resource-3.0.2", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support.test:monitor-1.0.2", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support.test:runner-1.0.2", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support:animated-vector-drawable-27.1.1", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:appcompat-v7-27.1.1", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-annotations:27.1.1@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-compat-27.1.1", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-core-ui-27.1.1", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-core-utils-27.1.1", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-fragment-27.1.1", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-vector-drawable-27.1.1", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.google.code.findbugs:jsr305:2.0.1@jar", DependencyScope.TEST)
                libraryDependency("Gradle: com.squareup:javawriter:2.1.1@jar", DependencyScope.TEST)
                libraryDependency("Gradle: javax.inject:javax.inject:1@jar", DependencyScope.TEST)
                libraryDependency("Gradle: junit:junit:4.12@jar", DependencyScope.TEST)
                libraryDependency("Gradle: net.sf.kxml:kxml2:2.3.0@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-core:1.3@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-integration:1.3@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-library:1.3@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0@jar", DependencyScope.COMPILE)
                moduleDependency("shared", DependencyScope.COMPILE)
                moduleDependency("shared_androidMain", DependencyScope.COMPILE)
                moduleDependency("shared_androidTest", DependencyScope.TEST)
                moduleDependency("shared_commonMain", DependencyScope.COMPILE)
                moduleDependency("shared_commonTest", DependencyScope.TEST)
            }
            module("shared")
            module("shared_commonMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:1.3.0-rc-146", DependencyScope.COMPILE)
                sourceFolder("shared/src/commonMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("shared/src/commonMain/resources", KotlinResourceRootType.Resource)
            }
            module("shared_commonTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:1.3.0-rc-146", DependencyScope.TEST)
                moduleDependency("shared_commonMain", DependencyScope.TEST)
                sourceFolder("shared/src/commonTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("shared/src/commonTest/resources", KotlinResourceRootType.TestResource)
            }
            module("shared_androidMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:1.3.0-rc-146", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:1.3.0-rc-146", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("shared_commonMain", DependencyScope.COMPILE)
                sourceFolder("shared/src/androidMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("shared/src/androidMain/resources", JavaResourceRootType.RESOURCE)
            }
            module("shared_androidTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:1.3.0-rc-146", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:1.3.0-rc-146", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("shared_androidMain", DependencyScope.TEST)
                moduleDependency("shared_commonMain", DependencyScope.TEST)
                moduleDependency("shared_commonTest", DependencyScope.TEST)
                sourceFolder("shared/src/androidTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("shared/src/androidTest/resources", JavaResourceRootType.TEST_RESOURCE)
            }
            module("shared_iOSMain") {
                libraryDependency("Gradle: Kotlin/Native:stdlib:0.9.3", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:1.3.0-rc-146", DependencyScope.COMPILE)
                moduleDependency("shared_commonMain", DependencyScope.COMPILE)
                sourceFolder("shared/src/iOSMain/kotlin", KotlinSourceRootType.Source)
                sourceFolder("shared/src/iOSMain/resources", KotlinResourceRootType.Resource)
            }
            module("shared_iOSTest") {
                libraryDependency("Gradle: Kotlin/Native:stdlib:0.9.3", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:1.3.0-rc-146", DependencyScope.TEST)
                moduleDependency("shared_iOSMain", DependencyScope.TEST)
                moduleDependency("shared_commonMain", DependencyScope.TEST)
                moduleDependency("shared_commonTest", DependencyScope.TEST)
                sourceFolder("shared/src/iOSTest/kotlin", KotlinSourceRootType.TestSource)
                sourceFolder("shared/src/iOSTest/resources", KotlinResourceRootType.TestResource)
            }
        }
    }

    private fun checkProjectStructure(
        exhaustiveModuleList: Boolean = true,
        exhaustiveSourceSourceRootList: Boolean = true,
        exhaustiveDependencyList: Boolean = true,
        body: ProjectInfo.() -> Unit = {}
    ) {
        checkProjectStructure(
            myProject,
            projectPath,
            exhaustiveModuleList,
            exhaustiveSourceSourceRootList,
            exhaustiveDependencyList,
            body)
    }

    override fun importProject() {
        val isCreateEmptyContentRootDirectories = currentExternalProjectSettings.isCreateEmptyContentRootDirectories
        currentExternalProjectSettings.isCreateEmptyContentRootDirectories = true
        try {
            super.importProject()
        } finally {
            currentExternalProjectSettings.isCreateEmptyContentRootDirectories = isCreateEmptyContentRootDirectories
        }
    }

    companion object {
        @Parameterized.Parameters(name = "{index}: with Gradle-{0}")
        @Throws(Throwable::class)
        @JvmStatic
        fun data() = listOf(arrayOf("4.7"))
    }
}