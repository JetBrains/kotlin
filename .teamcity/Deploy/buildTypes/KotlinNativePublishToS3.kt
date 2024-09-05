package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KotlinNativePublishToS3 : BuildType({
    name = "🐧 Publish Kotlin/Native to JB CDN (S3)"
    description = "Kotlin/Native distribution publish to JetBrains CDN with Amazon S3 (deploy)"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("kotlin.native.path.prefix", "builds/dev")
        param("env.AWS_SECRET_ACCESS_KEY", "%vault:download_jetbrains_com:aws-main/sts/download-kotlin-native-deployer!/secret_key%")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
        param("env.AWS_SESSION_TOKEN", "%vault:download_jetbrains_com:aws-main/sts/download-kotlin-native-deployer!/security_token%")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("kotlin.native.s3.path", "kotlin/native")
        param("env.AWS_ACCESS_KEY_ID", "%vault:download_jetbrains_com:aws-main/sts/download-kotlin-native-deployer!/access_key%")
        param("kotlin.native.bundle.location", "cdn-bundle")
        param("kotlin.native.s3.region", "eu-west-1")
        param("konanMetaVersion", "%build.number.native.meta.version%")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        param("kotlin.native.s3.bucket", "s3://download-prod-cdn.jetbrains.com")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "AWS S3 deployment to download.jetbrains.com"
            scriptContent = """
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-linux-x86_64-%kotlin.native.version%.tar.gz %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/linux-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-linux-x86_64-%kotlin.native.version%.tar.gz.sha256 %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/linux-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-linux-x86_64-%kotlin.native.version%.spdx.json %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/linux-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.tar.gz.sha256 %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/linux-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.tar.gz %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/linux-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.spdx.json %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/linux-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-macos-x86_64-%kotlin.native.version%.tar.gz %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-macos-x86_64-%kotlin.native.version%.tar.gz.sha256 %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-macos-x86_64-%kotlin.native.version%.spdx.json %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.tar.gz.sha256 %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.tar.gz %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.spdx.json %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-aarch64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz.sha256 %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-aarch64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-macos-aarch64-%kotlin.native.version%.spdx.json %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-aarch64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz.sha256 %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-aarch64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-aarch64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.spdx.json %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/macos-aarch64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-windows-x86_64-%kotlin.native.version%.zip %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/windows-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-windows-x86_64-%kotlin.native.version%.zip.sha256 %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/windows-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-windows-x86_64-%kotlin.native.version%.spdx.json %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/windows-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.zip.sha256 %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/windows-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.zip %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/windows-x86_64/ --acl private --region %kotlin.native.s3.region%
                aws s3 cp %kotlin.native.bundle.location%/kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.spdx.json %kotlin.native.s3.bucket%/%kotlin.native.s3.path%/%kotlin.native.path.prefix%/%kotlin.native.version%/windows-x86_64/ --acl private --region %kotlin.native.s3.region%
            """.trimIndent()
            dockerImage = "amazon/aws-cli"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
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
