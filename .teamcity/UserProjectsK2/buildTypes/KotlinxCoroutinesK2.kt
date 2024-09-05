package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object KotlinxCoroutinesK2 : BuildType({
    name = "🐧 [Project] kotlinx.coroutines with K2"

    artifactRules = """
                    **/*.hprof=>internal/hprof.zip
                    **/kotlin-daemon*.log=>internal/logs.zip
                    %env.GRADLE_USER_HOME%/**/*daemon*.log=>internal/logs.zip
        **/testKitCache/test-kit-daemon/**/*daemon*.log=>internal/test-kit-daemon-logs.zip
    """.trimIndent()
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("kotlin.artifacts.repository", "file://%teamcity.build.checkoutDir%/artifacts/kotlin/")
        param("env.CACHE_REDIRECTOR", "true")
    }

    vcs {
        root(_Self.vcsRoots.KotlinxCoroutinesK2VCS, "+:. => user-project")
        root(_Self.vcsRoots.CommunityProjectPluginVcs, "+:. => community-project-plugin")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        gradle {
            name = "build"
            tasks = "clean build check"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = """
                --no-build-cache --no-configuration-cache
                           --init-script %teamcity.build.checkoutDir%/community-project-plugin/cache-redirector.init.gradle
                           -x kotlinStoreYarnLock
                           -Pkotlin_repo_url=%kotlin.artifacts.repository%
                           -Pkotlin_version=${BuildNumber.depParamRefs["deployVersion"]}
                           -Pkotlin.native.version=${BuildNumber.depParamRefs["deployVersion"]}
                           -Pkotlin_language_version=2.1
                           -Pkotlin_api_version=2.1 --stacktrace --info --continue
            """.trimIndent()
            jdkHome = "%env.JDK_11_0%"
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
