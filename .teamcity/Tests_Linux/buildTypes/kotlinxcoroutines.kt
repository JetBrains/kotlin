package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object kotlinxcoroutines : BuildType({
    name = "🐧 [Project] kotlinx.coroutines K2"
    description = """
        https://youtrack.jetbrains.com/articles/KT-A-430/Merge-QG-Community-projects-compilation
        kotlinx.coroutines settings:
        VCS URL: git@github.com:Kotlin/kotlinx-coroutines.git
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
            tasks = "clean build -x check"
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
