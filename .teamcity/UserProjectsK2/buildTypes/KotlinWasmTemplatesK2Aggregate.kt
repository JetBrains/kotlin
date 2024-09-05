package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object KotlinWasmTemplatesK2Aggregate : BuildType({
    name = "🐧 [Aggregate] Kotlin Wasm Templates"
    description = """
        1st Step - https://github.com/Kotlin/kotlin-wasm-browser-template/tree/kotlin-community/k2/dev build
        2nd Step - https://github.com/Kotlin/kotlin-wasm-compose-template/tree/kotlin-community/k2/dev build 
        3rd Step - https://github.com/Kotlin/kotlin-wasm-nodejs-template/tree/kotlin-community/k2/dev build
        4rd Step - https://github.com/Kotlin/kotlin-wasm-nodejs-template/tree/kotlin-community/k2/dev development run
        5th Step - https://github.com/Kotlin/kotlin-wasm-nodejs-template/tree/kotlin-community/k2/dev production run
        6th Step - https://github.com/Kotlin/kotlin-wasm-wasi-template/tree/kotlin-community/k2/dev build 
        7th Step - https://github.com/Kotlin/kotlin-wasm-wasi-template/tree/kotlin-community/k2/dev development run
        8th Step - https://github.com/Kotlin/kotlin-wasm-wasi-template/tree/kotlin-community/k2/dev production run
        9th Step - https://github.com/Kotlin/kotlin-wasm-wasi-template/tree/kotlin-community/k2/dev deno development run
        10th Step - https://github.com/Kotlin/kotlin-wasm-wasi-template/tree/kotlin-community/k2/dev deno production run
    """.trimIndent()

    artifactRules = """
                    **/*.hprof=>internal/hprof.zip
                    **/kotlin-daemon*.log=>internal/logs.zip
                    %env.GRADLE_USER_HOME%/**/*daemon*.log=>internal/logs.zip
        **/testKitCache/test-kit-daemon/**/*daemon*.log=>internal/test-kit-daemon-logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.KotlinWasmBrowserTemplatesK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVcs, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(UserProjectsK2.vcsRoots.KotlinWasmComposeTemplatesK2VCS, "+:. => wasmCompose")
        root(UserProjectsK2.vcsRoots.KotlinWasmNodeJSTemplatesK2VCS, "+:. => wasmNodeJS")
        root(UserProjectsK2.vcsRoots.KotlinWasmWasiTemplatesK2VCS, "+:. => wasmWasi")
    }

    steps {
        gradle {
            name = "KotlinWasmBrowserTemplates build"
            tasks = "assemble"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
        gradle {
            name = "KotlinWasmComposeTemplates build"
            tasks = "wasmJsBrowserDistribution"
            buildFile = "build.gradle.kts"
            workingDir = "wasmCompose"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
        gradle {
            name = "KotlinWasmNodeJS build"
            tasks = "build"
            buildFile = "build.gradle.kts"
            workingDir = "wasmNodeJS"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
        gradle {
            name = "KotlinWasmNodeJS developmentRun"
            tasks = "wasmJsNodeDevelopmentRun"
            buildFile = "build.gradle.kts"
            workingDir = "wasmNodeJS"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
        gradle {
            name = "KotlinWasmNodeJS productionRun"
            tasks = "wasmJsNodeProductionRun"
            buildFile = "build.gradle.kts"
            workingDir = "wasmNodeJS"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
        gradle {
            name = "KotlinWasmWasi build"
            tasks = "build"
            buildFile = "build.gradle.kts"
            workingDir = "wasmWasi"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
        gradle {
            name = "KotlinWasmWasi developmentRun"
            tasks = "wasmWasiNodeDevelopmentRun"
            buildFile = "build.gradle.kts"
            workingDir = "wasmWasi"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
        gradle {
            name = "KotlinWasmWasi productionRun"
            tasks = "wasmWasiNodeProductionRun"
            buildFile = "build.gradle.kts"
            workingDir = "wasmWasi"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
        gradle {
            name = "KotlinWasmWasi deno developmentRun"
            tasks = "wasmWasiDenoDevelopmentRun"
            buildFile = "build.gradle.kts"
            workingDir = "wasmWasi"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
        gradle {
            name = "KotlinWasmWasi deno productionRun"
            tasks = "wasmWasiDenoProductionRun"
            buildFile = "build.gradle.kts"
            workingDir = "wasmWasi"
            gradleParams = """
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                            -Pkotlin_language_version=2.1  --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_17_0%"
        }
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.AFTER_BUILD
            paths = "+:user-project"
        }
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "kotlin-k2-user-projects-notifications"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            buildFailed = true
            firstFailureAfterSuccess = true
        }
    }

    dependencies {
        dependency(_Self.buildTypes.Artifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = "+:maven.zip!**=>artifacts/kotlin"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "aquarius-up-aws")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
