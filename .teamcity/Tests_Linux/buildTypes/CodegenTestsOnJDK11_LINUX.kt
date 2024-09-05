package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object CodegenTestsOnJDK11_LINUX : BuildType({
    name = "🐧 Test Codegen on 11 JDK"

    artifactRules = """
        **/build/reports/tests/**=>internal/test_results.zip
        **/build/test-results/**=>internal/test_results.zip
        **/hs_err*.log => internal/hs_err.zip
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("gradleParameters", "%globalGradleParameters% -Pkotlin.build.testRetry.maxRetries=0")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk11", "%env.JDK_11_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Build codegen tests for jvmTarget=8, run tests on JVM 11"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = ":compiler:tests-different-jdk:codegenTarget8Jvm11Test"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Pkotlin.build.disable.verification.tasks=true -Dscan.tag.verification-tasks-disabled"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Codegen for jvmTarget=8, run tests on JVM 11"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = ":compiler:tests-different-jdk:codegenTarget8Jvm11Test"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel --continue"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Build codegen tests for jvmTarget=11, run tests on JVM 11"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = ":compiler:tests-different-jdk:codegenTarget11Jvm11Test"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Pkotlin.build.disable.verification.tasks=true -Dscan.tag.verification-tasks-disabled"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Codegen for jvmTarget=11, run tests on JVM 11"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = ":compiler:tests-different-jdk:codegenTarget11Jvm11Test"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --no-parallel --continue"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 90
        supportTestRetry = true
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
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(_Self.buildTypes.CompileAllClasses) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
