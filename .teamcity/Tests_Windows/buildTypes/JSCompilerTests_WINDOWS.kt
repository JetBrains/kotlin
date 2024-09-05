package Tests_Windows.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.maven

object JSCompilerTests_WINDOWS : BuildType({
    name = "🪟 JS Compiler Tests"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
        **/build/reports/tests/**=>internal/test_results.zip
        **/build/test-results/**=>internal/test_results.zip
        **/kotlin.test/js/it/build/*.log=>internal/test_results.zip
        **/build.log=>internal/test_results.zip
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        param("system.deployVersion", "%DeployVersion%")
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("testTasks", "jsCompilerTest")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        maven {
            name = "Set Version"
            goals = "versions:set"
            pomLocation = "%teamcity.build.checkoutDir%/kotlin/libraries/pom.xml"
            runnerArgs = "-DnewVersion=%DeployVersion% -DgenerateBackupPoms=false -DprocessAllModules=true"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            mavenVersion = bundled_3_6()
            localRepoScope = MavenBuildStep.RepositoryScope.BUILD_CONFIGURATION
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Build tests"
            tasks = "%testTasks%"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Pkotlin.build.disable.verification.tasks=true -Dscan.tag.verification-tasks-disabled"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Run tests"
            tasks = "%testTasks%"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel --continue -Pkotlin.build.cache.tests.disabled=%teamcity.build.branch.is_default%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 300
        supportTestRetry = true
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.DISABLED
            lockingProcesses = Swabra.LockingProcessPolicy.KILL
        }
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
        snapshot(_Self.buildTypes.CompileAllClasses) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Windows")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
