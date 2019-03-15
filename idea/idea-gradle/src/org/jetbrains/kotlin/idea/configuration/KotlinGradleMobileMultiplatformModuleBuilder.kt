/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.configuration.xcode.XcodeProjectConfigurator
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import java.io.BufferedWriter

class KotlinGradleMobileMultiplatformModuleBuilder :
    KotlinGradleAbstractMultiplatformModuleBuilder(mppInApplication = true) {

    private val commonName: String = "common"
    private var jvmTargetName: String = "android"
    private var nativeTargetName: String = "ios"
    private val androidAppName = mppDirName

    private val commonSourceName get() = "$commonName$productionSuffix"
    private val commonTestName get() = "$commonName$testSuffix"
    private val jvmSourceName get() = "$jvmTargetName$productionSuffix"
    private val jvmTestName get() = "$jvmTargetName$testSuffix"
    private val nativeSourceName get() = "$nativeTargetName$productionSuffix"
    private val nativeTestName get() = "$nativeTargetName$testSuffix"
    private val mainSourceName get() = productionSuffix.toLowerCase()
    private val mainTestName get() = testSuffix.toLowerCase()

    override fun getBuilderId() = "kotlin.gradle.multiplatform.mobile"

    override fun getPresentableName() = "Mobile Android/iOS | Gradle"

    override fun getDescription() =
        "Multiplatform Gradle project allowing reuse of the same Kotlin code between Android and iOS mobile platforms"

    override fun BuildScriptDataBuilder.setupAdditionalDependencies() {
        addBuildscriptDependencyNotation("classpath 'com.android.tools.build:gradle:3.2.0'")
        addBuildscriptRepositoriesDefinition("google()")
        addBuildscriptRepositoriesDefinition("jcenter()")
        addRepositoriesDefinition("google()")
        addRepositoriesDefinition("jcenter()")
    }

    override fun BuildScriptDataBuilder.setupAdditionalDependenciesForApplication() {
        addRepositoriesDefinition("google()")
        addRepositoriesDefinition("jcenter()")
    }

    override fun createProjectSkeleton(rootDir: VirtualFile) {
        val appDir = rootDir.findChild(androidAppName)!!
        val src = appDir.createChildDirectory(this, "src")

        val commonMain = src.createKotlinSampleFileWriter(commonSourceName)
        val commonTest = src.createKotlinSampleFileWriter(commonTestName, fileName = "SampleTests.kt")
        val androidMain = src.createKotlinSampleFileWriter(mainSourceName, jvmTargetName, languageName = "java")
        val androidTest = src.createKotlinSampleFileWriter(mainTestName, languageName = "java", fileName = "SampleTestsAndroid.kt")

        val androidLocalProperties = rootDir.createChildData(this, "local.properties").bufferedWriter()
        val androidRoot = src.findChild(mainSourceName)!!
        val androidManifest = androidRoot.createChildData(this, "AndroidManifest.xml").bufferedWriter()
        val androidResources = androidRoot.createChildDirectory(this, "res")
        val androidValues = androidResources.createChildDirectory(this, "values")
        val androidLayout = androidResources.createChildDirectory(this, "layout")
        val androidStrings = androidValues.createChildData(this, "strings.xml").bufferedWriter()
        val androidStyles = androidValues.createChildData(this, "styles.xml").bufferedWriter()
        val androidActivityMain = androidLayout.createChildData(this, "activity_main.xml").bufferedWriter()

        val nativeMain = src.createKotlinSampleFileWriter(nativeSourceName, nativeTargetName)
        val nativeTest = src.createKotlinSampleFileWriter(nativeTestName, fileName = "SampleTestsIOS.kt")

        try {
            commonMain.write(
                """
                package sample

                expect class Sample() {
                    fun checkMe(): Int
                }

                expect object Platform {
                    val name: String
                }

                fun hello(): String = "Hello from ${"$"}{Platform.name}"

                class Proxy {
                    fun proxyHello() = hello()
                }

                fun main() {
                    println(hello())
                }
            """.trimIndent()
            )

            androidMain.write(
                """
                package sample

                import android.support.v7.app.AppCompatActivity
                import android.os.Bundle
                import android.widget.TextView

                actual class Sample {
                    actual fun checkMe() = 44
                }

                actual object Platform {
                    actual val name: String = "Android"
                }

                class MainActivity : AppCompatActivity() {

                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        Sample().checkMe()
                        setContentView(R.layout.activity_main)
                        findViewById<TextView>(R.id.main_text).text = hello()
                    }
                }
            """.trimIndent()
            )

            nativeMain.write(
                """
                package sample

                actual class Sample {
                    actual fun checkMe() = 7
                }

                actual object Platform {
                    actual val name: String = "iOS"
                }
            """.trimIndent()
            )

            commonTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTests {
                    @Test
                    fun testMe() {
                        assertTrue(Sample().checkMe() > 0)
                    }

                    @Test
                    fun testProxy() {
                        assertTrue(Proxy().proxyHello().isNotEmpty())
                    }
                }
            """.trimIndent()
            )

            androidTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTestsAndroid {
                    @Test
                    fun testHello() {
                        assertTrue("Android" in hello())
                    }
                }
            """.trimIndent()
            )

            nativeTest.write(
                """
                package sample

                import kotlin.test.Test
                import kotlin.test.assertTrue

                class SampleTestsIOS {
                    @Test
                    fun testHello() {
                        assertTrue("iOS" in hello())
                    }
                }
            """.trimIndent()
            )

            androidLocalProperties.write(
                """
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
# For customization when using a Version Control System, please read the
# header note.
sdk.dir=PleaseSpecifyAndroidSdkPathHere
            """.trimIndent()
            )

            androidManifest.write(
                """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="sample">

    <application
            android:allowBackup="true"
            android:label="@string/app_name"
            android:supportsRtl="true"
            android:theme="@style/AppTheme">
        <activity android:name="sample.MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
            """.trimIndent()
            )

            androidStrings.write(
                """
<resources>
    <string name="app_name">android-app</string>
</resources>
            """.trimIndent()
            )

            androidStyles.write(
                """
<resources>
    <!-- Base application theme. -->
    <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
        <!-- Customize your theme here. -->
    </style>
</resources>
            """.trimIndent()
            )

            androidActivityMain.write(
                """
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <TextView
        android:id="@+id/main_text"
        android:textSize="42sp"
        android:layout_margin="5sp"
        android:textAlignment="center"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</android.support.constraint.ConstraintLayout>
            """.trimIndent()
            )
        } finally {
            listOf(
                commonMain, commonTest, androidMain, androidTest, nativeMain, nativeTest,
                androidLocalProperties, androidManifest, androidStrings, androidStyles, androidActivityMain
            ).forEach(BufferedWriter::close)
        }

        XcodeProjectConfigurator().createSkeleton(rootDir)
    }


    override fun buildMultiPlatformPart(): String {
        return """
            apply plugin: 'com.android.application'
            apply plugin: 'kotlin-android-extensions'

            android {
                compileSdkVersion 28
                defaultConfig {
                    applicationId 'org.jetbrains.kotlin.mpp_app_android'
                    minSdkVersion 15
                    targetSdkVersion 28
                    versionCode 1
                    versionName '1.0'
                    testInstrumentationRunner 'android.support.test.runner.AndroidJUnitRunner'
                }
                buildTypes {
                    release {
                        minifyEnabled false
                    }
                }
            }

            dependencies {
                implementation fileTree(dir: 'libs', include: ['*.jar'])
                implementation 'com.android.support:appcompat-v7:28.0.0'
                implementation 'com.android.support.constraint:constraint-layout:1.1.3'
                androidTestImplementation 'com.android.support.test:runner:1.0.2'
            }

            kotlin {
                android("$jvmTargetName")
                // This is for iPhone emulator
                // Switch here to iosArm64 (or iosArm32) to build library for iPhone device
                iosX64("$nativeTargetName") {
                    binaries {
                        framework()
                    }
                }
                sourceSets {
                    $commonSourceName {
                        dependencies {
                            implementation kotlin('stdlib-common')
                        }
                    }
                    $commonTestName {
                        dependencies {
                    		implementation kotlin('test-common')
                    		implementation kotlin('test-annotations-common')
                        }
                    }
                    $jvmSourceName {
                        dependencies {
                            implementation kotlin('stdlib')
                        }
                    }
                    $jvmTestName {
                        dependencies {
                            implementation kotlin('test')
                            implementation kotlin('test-junit')
                        }
                    }
                    $nativeSourceName {
                    }
                    $nativeTestName {
                    }
                }
            }

            // This task attaches native framework built from ios module to Xcode project
            // (see iosApp directory). Don't run this task directly,
            // Xcode runs this task itself during its build process.
            // Before opening the project from iosApp directory in Xcode,
            // make sure all Gradle infrastructure exists (gradle.wrapper, gradlew).
            task copyFramework {
                def buildType = project.findProperty('kotlin.build.type') ?: 'DEBUG'
                def target = project.findProperty('kotlin.target') ?: 'ios'
                dependsOn kotlin.targets."${"$"}target".binaries.getFramework(buildType).linkTask

                doLast {
                    def srcFile = kotlin.targets."${"$"}target".binaries.getFramework(buildType).outputFile
                    def targetDir = getProperty('configuration.build.dir')
                    copy {
                        from srcFile.parent
                        into targetDir
                        include '$androidAppName.framework/**'
                        include '$androidAppName.framework.dSYM'
                    }
                }
            }
        """.trimIndent()
    }
}