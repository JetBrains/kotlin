package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object SpaceK2 : BuildType({
    name = "🐧 [Project] Space with K2"

    artifactRules = """
                    **/*.hprof=>internal/hprof.zip
                    **/kotlin-daemon*.log=>internal/logs.zip
                    %env.GRADLE_USER_HOME%/**/*daemon*.log=>internal/logs.zip
        **/testKitCache/test-kit-daemon/**/*daemon*.log=>internal/test-kit-daemon-logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("docker.image", "amazoncorretto:17")
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        password("env.JB_SPACE_CLIENT_SECRET", "credentialsJSON:6d2dd689-93a7-4d0b-8175-4f470c0639c0")
        param("env.JB_SPACE_CLIENT_ID", "%space.packages.user%")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.SpaceK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVcs, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "Space Kotlin version replacement in libs.versions.toml"
            scriptContent = """
                #!/bin/bash
                sed -i -e 's#kotlin-lang = \".*\"#kotlin-lang = \"${BuildNumber.depParamRefs["deployVersion"]}\"#g' user-project/gradle/libs.versions.toml
                sed -i -e 's#kotlin-plugin = \".*\"#kotlin-plugin = \"${BuildNumber.depParamRefs["deployVersion"]}\"#g' user-project/gradle/libs.versions.toml
                
                echo "replaced Kotlin parameters in libs.versions.toml config"
                cat user-project/gradle/libs.versions.toml | grep -E '^kotlin-(lang|plugin) = \".*\"'
            """.trimIndent()
        }
        gradle {
            name = "beforePush"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = "beforePush"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --configuration-cache-problems=warn
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/community-project.init.gradle.kts
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle
                                        -Pcommunity.project.kotlin.repo=%kotlin.artifacts.repository%
                                        -Pcommunity.project.kotlin.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pcommunity.project.kotlin.languageVersion=2.1 
                                        -Pcommunity.project.kotlin.apiVersion=2.1
                                        --dry-run --stacktrace --info --continue
            """.trimIndent()
            dockerImage = "%docker.image%"
        }
        gradle {
            name = "js compile"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = "compileKotlinJs compileDevelopmentExecutableKotlinJs compileProductionExecutableKotlinJs"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --configuration-cache-problems=warn
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/community-project.init.gradle.kts
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle
                                        -Pcommunity.project.kotlin.repo=%kotlin.artifacts.repository%
                                        -Pcommunity.project.kotlin.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pcommunity.project.kotlin.languageVersion=2.1 
                                        -Pcommunity.project.kotlin.apiVersion=2.1 --stacktrace --info --continue
            """.trimIndent()
            dockerImage = "%docker.image%"
        }
        gradle {
            name = "jvm compile"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = "compileKotlinJvm"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --configuration-cache-problems=warn
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/community-project.init.gradle.kts
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle
                                        -Pcommunity.project.kotlin.repo=%kotlin.artifacts.repository%
                                        -Pcommunity.project.kotlin.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pcommunity.project.kotlin.languageVersion=2.1 
                                        -Pcommunity.project.kotlin.apiVersion=2.1 --stacktrace --info --continue
            """.trimIndent()
            dockerImage = "%docker.image%"
        }
        gradle {
            name = "tests compile"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = "compileTestKotlin compileTestKotlinJvm compileTestJava"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = """
                --configuration-cache-problems=warn
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/community-project.init.gradle.kts
                                        --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle
                                        -Pcommunity.project.kotlin.repo=%kotlin.artifacts.repository%
                                        -Pcommunity.project.kotlin.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]}
                                        -Pcommunity.project.kotlin.languageVersion=2.1 
                                        -Pcommunity.project.kotlin.apiVersion=2.1 --stacktrace --info --continue
            """.trimIndent()
            dockerImage = "%docker.image%"
        }
    }

    failureConditions {
        executionTimeoutMin = 120
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
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_774"
            }
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
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "31000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "33000")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
