package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KotlinNativePublishMaven : BuildType({
    name = "🐧 Publish Kotlin/Native to Maven"
    description = "Kotlin/Native distribution publish to maven (deploy)"

    artifactRules = "kotlin/build/local-publish=>local-publish-maven.zip"
    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "%globalGradleParameters% -Pbuild.number=%kotlin.native.version% -Pkotlin.native.enabled=true --no-configuration-cache")
        param("kotlin.native.path.prefix", "builds/dev")
        select("deploy-repo", "local", display = ParameterDisplay.PROMPT,
                options = listOf("local", "kotlin-space-repo" to "kotlin-space-packages"))
        password("system.kotlin.key.passphrase", "credentialsJSON:fab0f148-ce2d-46d7-a828-5eafb495ad98", display = ParameterDisplay.HIDDEN)
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        password("system.kotlin.kotlin-space-packages.user", "credentialsJSON:70dd5a56-00b7-43a9-a2b7-e9c802b5d17d", display = ParameterDisplay.HIDDEN)
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
        password("system.kotlin.kotlin-space-packages.password", "credentialsJSON:6a448008-2992-4f63-9a64-2e5013887a2e", display = ParameterDisplay.HIDDEN)
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        select("deploy-url", "file://%teamcity.build.checkoutDir%/kotlin/build/local-publish", display = ParameterDisplay.PROMPT,
                options = listOf("Local directory build/local-publish" to "file://%teamcity.build.checkoutDir%/kotlin/build/local-publish", "kotlin.space (kotlin dev)" to "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev", "kotlin.space (kotlin bootstrap)" to "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap"))
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("system.kotlin.key.name", "%sign.key.id.new%")
        param("kotlin.native.bundle.location", "cdn-bundle")
        param("konanMetaVersion", "%build.number.native.meta.version%")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Prepare gnupg"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            scriptContent = """
                cd libraries
                export HOME=${'$'}(pwd)
                export GPG_TTY=${'$'}(tty)
                
                rm -rf .gnupg
                
                cat >keyfile <<EOT
                %sign.key.private.new%
                EOT
                gpg --allow-secret-key-import --batch --import keyfile
                rm -v keyfile
                
                cat >keyfile <<EOT
                %sign.key.main.public%
                EOT
                gpg --batch --import keyfile
                rm -v keyfile
            """.trimIndent()
        }
        gradle {
            name = "Publish K/N bundles to Maven repository"
            tasks = ":kotlin-native:publish"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel -PsigningRequired=true -Psigning.gnupg.executable=gpg -Psigning.gnupg.keyName=%sign.key.id.new% -Psigning.gnupg.homeDir=%teamcity.build.checkoutDir%/kotlin/libraries/.gnupg -Psigning.gnupg.passphrase=%sign.key.passphrase.new% -Pdeploy-repo=%deploy-repo% -Pdeploy-url=%deploy-url% -PdeployVersion=${BuildNumber.depParamRefs["deployVersion"]} --no-scan --info --stacktrace --no-build-cache -PnativeBundlesLocation=%teamcity.build.checkoutDir%/%kotlin.native.bundle.location%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Publish K/N embeddable Jar to Maven repository"
            tasks = ":kotlin-native:prepare:kotlin-native-compiler-embeddable:publish"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel -PsigningRequired=true -Psigning.gnupg.executable=gpg -Psigning.gnupg.keyName=%sign.key.id.new% -Psigning.gnupg.homeDir=%teamcity.build.checkoutDir%/kotlin/libraries/.gnupg -Psigning.gnupg.passphrase=%sign.key.passphrase.new% -Pdeploy-repo=%deploy-repo% -Pdeploy-url=%deploy-url% -PdeployVersion=${BuildNumber.depParamRefs["deployVersion"]} --no-scan --info --stacktrace --no-build-cache"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    features {
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_linux_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = """
                    kotlin-native-linux-x86_64-%kotlin.native.version%.tar.gz => %kotlin.native.bundle.location%
                    kotlin-native-linux-x86_64-%kotlin.native.version%.tar.gz.sha256 => %kotlin.native.bundle.location%
                    kotlin-native-linux-x86_64-%kotlin.native.version%.spdx.json => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.tar.gz.sha256 => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.tar.gz => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.spdx.json => %kotlin.native.bundle.location%
                """.trimIndent()
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = """
                    kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz => %kotlin.native.bundle.location%
                    kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz.sha256 => %kotlin.native.bundle.location%
                    kotlin-native-macos-aarch64-%kotlin.native.version%.spdx.json => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz.sha256 => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.spdx.json => %kotlin.native.bundle.location%
                """.trimIndent()
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = """
                    kotlin-native-macos-x86_64-%kotlin.native.version%.tar.gz => %kotlin.native.bundle.location%
                    kotlin-native-macos-x86_64-%kotlin.native.version%.tar.gz.sha256 => %kotlin.native.bundle.location%
                    kotlin-native-macos-x86_64-%kotlin.native.version%.spdx.json => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.tar.gz.sha256 => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.tar.gz => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.spdx.json => %kotlin.native.bundle.location%
                """.trimIndent()
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_mingw_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = """
                    kotlin-native-windows-x86_64-%kotlin.native.version%.zip => %kotlin.native.bundle.location%
                    kotlin-native-windows-x86_64-%kotlin.native.version%.zip.sha256 => %kotlin.native.bundle.location%
                    kotlin-native-windows-x86_64-%kotlin.native.version%.spdx.json => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.zip.sha256 => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.zip => %kotlin.native.bundle.location%
                    kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.spdx.json => %kotlin.native.bundle.location%
                """.trimIndent()
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-deployment")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
