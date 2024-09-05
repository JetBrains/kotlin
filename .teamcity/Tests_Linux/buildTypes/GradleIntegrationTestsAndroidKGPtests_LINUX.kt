package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.parallelTests
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript

object GradleIntegrationTestsAndroidKGPtests_LINUX : BuildType({
    name = "🐧 Gradle Integration Tests Android KGP tests (Linux)"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
        **/build/reports/tests/**=>internal/test_results.zip
        **/build/test-results/**=>internal/test_results.zip
        %env.GRADLE_USER_HOME%/**/*daemon*.log=>internal/logs.zip
        **/testKitCache/test-kit-daemon/**/*daemon*.log=>internal/test-kit-daemon-logs.zip
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("mavenParameters", "")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        param("system.maven.repo.local", "%teamcity.build.checkoutDir%/dist/local-repo")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Build and install artifacts to local maven repo"
            tasks = "clean install"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Dmaven.repo.local=%teamcity.build.checkoutDir%/maven/repo -Pkotlin.build.gradle.publish.javadocs=false"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Build Gradle plugin integration tests"
            tasks = ":kotlin-gradle-plugin-integration-tests:kgpAndroidTestsGroupedByGradleVersion"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Pkotlin.build.disable.verification.tasks=true -Dscan.tag.verification-tasks-disabled"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Run Gradle plugin integration tests"
            tasks = ":kotlin-gradle-plugin-integration-tests:kgpAndroidTestsGroupedByGradleVersion"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel -Dmaven.repo.local=%teamcity.build.checkoutDir%/maven/repo -DkonanDataDirForIntegrationTests=%teamcity.build.checkoutDir%/kotlin/.kotlin/konan-for-gradle-tests -DkotlinNativeVersionForGradleIT=${BuildNumber.depParamRefs["deployVersion"]} -Pkotlin.build.gradle.publish.javadocs=false"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        kotlinScript {
            name = "Delete local maven repo"
            content = "java.io.File(args[0]).deleteRecursively()"
            arguments = "%teamcity.build.checkoutDir%/maven/repo"
        }
    }

    failureConditions {
        executionTimeoutMin = 180
        supportTestRetry = true
    }

    features {
        freeDiskSpace {
            requiredSpace = "15gb"
            failBuild = false
        }
        swabra {
            lockingProcesses = Swabra.LockingProcessPolicy.KILL
        }
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "#kotlin-build-tools-build-notifications"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            buildFailed = true
            firstFailureAfterSuccess = true
        }
        parallelTests {
            numberOfBatches = 4
        }
        perfmon {
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(_Self.buildTypes.CompileAllClasses) {
            onDependencyFailure = FailureAction.FAIL_TO_START
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
