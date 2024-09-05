package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object KotlinxDateTimeK2 : BuildType({
    name = "🍏ᵐ [Project] kotlinx-datetime with K2"

    artifactRules = """
                    **/*.hprof=>internal/hprof.zip
                    **/kotlin-daemon*.log=>internal/logs.zip
                    %env.GRADLE_USER_HOME%/**/*daemon*.log=>internal/logs.zip
        **/testKitCache/test-kit-daemon/**/*daemon*.log=>internal/test-kit-daemon-logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("kotlin.user.project.api.version", "2.1")
        param("kotlin.user.project.language.version", "2.1")
        param("env.GRADLE_USER_HOME", "%teamcity.agent.home.dir%/system/gradle")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.KotlinxDateTimeK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVcs, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        gradle {
            name = "build"
            tasks = "clean build"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = """
                -x kotlinStoreYarnLock
                --no-build-cache --no-configuration-cache
                --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle
                -Pkotlin_repo_url=%kotlin.artifacts.repository%
                -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                -Pkotlin_language_version=%kotlin.user.project.language.version%
                -Pkotlin_api_version=%kotlin.user.project.api.version%
                -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]}
                -Pjava.mainToolchainVersion=11
                -PoverrideTeamCityBuildNumber=false --stacktrace --info --continue
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 20
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
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        contains("system.cloud.profile_id", "aquarius-up-k8s")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "7000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "9000")
    }
})
