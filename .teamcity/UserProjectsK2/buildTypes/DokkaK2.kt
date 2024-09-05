package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object DokkaK2 : BuildType({
    name = "🐧 [Project] Dokka with K2"

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
    }

    vcs {
        root(UserProjectsK2.vcsRoots.DokkaK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVcs, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        gradle {
            name = "Build"
            tasks = "build"
            workingDir = "user-project"
            gradleParams = """
                --stacktrace --info
                                       --configuration-cache-problems=warn
                                       -Pkotlin_repo=%kotlin.artifacts.repository%
                                       -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                                       -Porg.jetbrains.dokka.kotlinLanguageLevel=2.1 --stacktrace --info --continue
            """.trimIndent()
            dockerImage = "%docker.image%"
            dockerRunParameters = "--user=%env.UID%:%env.UID% --volume /etc/passwd:/etc/passwd --volume %env.HOME%:%env.HOME%"
        }
    }

    failureConditions {
        executionTimeoutMin = 30
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
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
