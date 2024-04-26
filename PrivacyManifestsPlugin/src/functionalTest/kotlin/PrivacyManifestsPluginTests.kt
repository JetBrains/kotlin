
import junit.framework.TestCase.assertEquals
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.createFile
import kotlin.io.path.exists

class PrivacyManifestsPluginTests {

    @Test
    fun embedAndSign() {
        val testName = "embedAndSign"
        val appName = "My.app"
        val frameworks = "Frameworks"
        val projectDir = File("build/functionalTest").resolve(testName)
        val embedAndSignOutputs = projectDir.resolve("embedAndSignOutputs")
        embedAndSignOutputs.resolve(appName).resolve(frameworks).mkdirs()

        buildProject(
            projectDir = projectDir,
            kotlinVersion = kotlinVersion,
            buildScript = """
                kotlin {
                    iosArm64 {
                        binaries.framework {}
                    }
                    
                    privacyManifestConfiguration {
                        embedPrivacyManifest(
                            layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            """.trimIndent(),
            gradleArguments = arrayOf(
                ":embedAndSign", "--info"
            ),
            environmentVariables = System.getenv() + mapOf(
                "ENABLE_USER_SCRIPT_SANDBOXING" to "NO",
                "CONFIGURATION" to "Debug",
                "ARCHS" to "arm64",
                "SDK_NAME" to "iphoneos",
                "FRAMEWORKS_FOLDER_PATH" to "${appName}/${frameworks}",
                "UNLOCALIZED_RESOURCES_FOLDER_PATH" to appName,
                "TARGET_BUILD_DIR" to embedAndSignOutputs.canonicalPath,
            )
        )

        assertEquals(
            embedAndSignOutputs.resolve("My.app/KotlinMultiplatformPrivacyManifest.bundle/PrivacyInfo.xcprivacy").readText(),
            "test"
        )
    }

    @Test
    fun xcframework() {
        val testName = "xcframework"
        val projectDir = File("build/functionalTest").resolve(testName)

        buildProject(
            projectDir = projectDir,
            kotlinVersion = kotlinVersion,
            buildScript = """
                import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
                kotlin {
                    val xcf = XCFramework()
                    listOf(
                        // Thin
                        iosArm64(),
                
                
                        // Universal
                        watchosSimulatorArm64(),
                        watchosX64(),
                    ).forEach {
                        it.binaries.framework {
                            xcf.add(this)
                        }
                    }
                    
                    privacyManifestConfiguration {
                        embedPrivacyManifest(
                            layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            """.trimIndent(),
            gradleArguments = arrayOf(
                ":assembleXCFramework", "--info"
            ),
        )

        assertEquals(
            projectDir.resolve("build/XCFrameworks/release/${testName}.xcframework/ios-arm64/${testName}.framework/PrivacyInfo.xcprivacy").readText(),
            "test"
        )
        assertEquals(
            projectDir.resolve("build/XCFrameworks/release/${testName}.xcframework/watchos-arm64_x86_64-simulator/${testName}.framework/PrivacyInfo.xcprivacy").readText(),
            "test"
        )
    }

    @Test
    fun `cocoapods xcframework`() {
        val testName = "xcframeworkCocoaPods"
        val projectDir = File("build/functionalTest").resolve(testName)

        buildProject(
            projectDir = projectDir,
            kotlinVersion = kotlinVersion,
            otherPlugins = """
                kotlin("native.cocoapods") version "$kotlinVersion"
            """.trimIndent(),
            buildScript = """
                import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
                kotlin {
                    // Thin
                    iosArm64()
            
                    // Universal
                    watchosSimulatorArm64()
                    watchosX64()
                    
                    cocoapods {
                        version = "1.0"
                    }
                    
                    privacyManifestConfiguration {
                        embedPrivacyManifest(
                            layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            """.trimIndent(),
            gradleArguments = arrayOf(
                ":podPublishReleaseXCFramework", "--info"
            ),
        )

        assertEquals(
            projectDir.resolve("build/cocoapods/publish/release/${testName}.xcframework/ios-arm64/${testName}.framework/PrivacyInfo.xcprivacy").readText(),
            "test"
        )
        assertEquals(
            projectDir.resolve("build/cocoapods/publish/release/${testName}.xcframework/watchos-arm64_x86_64-simulator/${testName}.framework/PrivacyInfo.xcprivacy").readText(),
            "test"
        )
    }

    @Test
    fun `cocoapods syncFramework`() {
        val testName = "cocoaPodsSyncFramework"
        val projectDir = File("build/functionalTest").resolve(testName)

        buildProject(
            projectDir = projectDir,
            kotlinVersion = kotlinVersion,
            otherPlugins = """
                kotlin("native.cocoapods") version "$kotlinVersion"
            """.trimIndent(),
            buildScript = """
                import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
                kotlin {
                    // Thin
                    iosArm64()
            
                    // Universal
                    watchosSimulatorArm64()
                    watchosX64()
                    
                    cocoapods {
                        version = "1.0"
                    }
                    
                    privacyManifestConfiguration {
                        embedPrivacyManifest(
                            layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile
                        )
                    }
                }
            """.trimIndent(),
            gradleArguments = arrayOf(
                ":podspec", ":syncFramework",
                "-Pkotlin.native.cocoapods.configuration=Debug", "-Pkotlin.native.cocoapods.archs=arm64", "-Pkotlin.native.cocoapods.platform=iphoneos",
                "-Pkotlin.native.cocoapods.generate.wrapper=true",
            ),
        )

        assert(
            projectDir.resolve("build/cocoapods/framework/${testName}.framework").exists(),
        )
        assert(
            !projectDir.resolve("build/cocoapods/framework/${testName}.framework/PrivacyInfo.xcprivacy").exists(),
        )
        assert(
            projectDir.resolve("${testName}.podspec").readText().contains("spec.resource_bundles = {'KotlinMultiplatformPrivacyManifest' => ['PrivacyInfo.xcprivacy']}"),
        )
    }

    private val kotlinVersion = "1.9.23"

    private fun buildProject(
        projectDir: File,
        kotlinVersion: String,
        otherPlugins: String = "",
        buildScript: String,
        gradleArguments: Array<String>,
        environmentVariables: Map<String, String> = emptyMap(),
    ) {
        Files.createDirectories(projectDir.toPath())

        writeString(
            File(projectDir, "settings.gradle.kts"),
            """
                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                    }
                }
                
                pluginManagement {
                    repositories {
                        maven("file://${projectDir.absoluteFile.parentFile.parentFile.parentFile.resolve("repo").canonicalPath}")
                        gradlePluginPortal()
                    }
                }
            """.trimIndent()
        )

        writeString(
            File(projectDir, "build.gradle.kts"),
            """
                plugins {
                    id("org.kmp_apple_privacy_manifests.publication") version "unspecified"
                    kotlin("multiplatform") version "$kotlinVersion"
                    $otherPlugins
                }

                $buildScript
            """.trimIndent()
        )

        writeString(
            File(projectDir, "PrivacyInfo.xcprivacy"),
            "test"
        )

        val sources = projectDir.resolve("src/commonMain/kotlin").toPath()
        if (!sources.exists()) {
            Files.createDirectories(sources)
            sources.resolve("stub.kt").createFile()
        }

        GradleRunner.create()
            .forwardOutput()
            .withEnvironment(System.getenv() + environmentVariables)
            .withArguments(*gradleArguments)
            .withProjectDir(projectDir)
            .build()
    }

    @Throws(IOException::class)
    private fun writeString(file: File, string: String) {
        FileWriter(file).use { writer ->
            writer.write(string)
        }
    }
}