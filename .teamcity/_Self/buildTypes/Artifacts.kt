package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon

object Artifacts : BuildType({
    name = "🐧 Artifacts"

    artifactRules = """
        +:internal/** => internal
        +:kotlin-compiler-*
        +:kotlin-native-prebuilt-linux-x86_64-*
        +:kotlin-native-prebuilt-macos-aarch64-*
        +:maven => maven.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
        cleanCheckout = true
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
        dependency(CompilerDistAndMavenArtifacts) {
            snapshot {
            }

            artifacts {
                artifactRules = """
                    internal/**=>internal
                    maven.zip!**=>maven
                    kotlin-compiler-*
                """.trimIndent()
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_linux_x64_BUNDLE) {
            snapshot {
            }

            artifacts {
                artifactRules = """
                    +:native-maven/** => maven
                    +:kotlin-native-prebuilt-linux-x86_64-*
                """.trimIndent()
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE) {
            snapshot {
            }

            artifacts {
                artifactRules = """
                    +:native-maven/** => maven
                    +:kotlin-native-prebuilt-macos-aarch64-*
                """.trimIndent()
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
