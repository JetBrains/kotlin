package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object BenchmarkCompilationCheckK2 : BuildType({
    name = "🐧 [Project] kotlinx.benchmark K2"
    description = """
        https://youtrack.jetbrains.com/articles/KT-A-430/Merge-QG-Community-projects-compilation
        kotlinx.benchmark settings:
        VCS URL: git@github.com:Kotlin/kotlinx.benchmark.git 
        VCS branch: kotlin-community/dev
        Deploy Kotlin Maven Artifacts (MANUAL): https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_BuildPlayground_Aquarius_DeployMavenArtifacts
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
        root(_Self.vcsRoots.benchmarkForAggregateVCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVcs, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        gradle {
            name = "build project"
            tasks = "build"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --stacktrace --info -x check -x test 
                                        -x kotlinStoreYarnLock 
                                        -PoverrideTeamCityBuildNumber=false 
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle
                                        -Pkotlin_repo_url=%kotlin.artifacts.repository%
                                        -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin_language_version=2.1
                                        -Pkotlin_api_version=2.1
            """.trimIndent()
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Process description in case of build failures"
            executionMode = BuildStep.ExecutionMode.RUN_ONLY_ON_FAILURE
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                echo "##teamcity[buildStatus text='More info: https://youtrack.jetbrains.com/articles/KT-A-430/Merge-QG-Community-projects-compilation {build.status.text}']"
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 45
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.AFTER_BUILD
            paths = "+:user-project"
        }
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
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
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
