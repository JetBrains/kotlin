package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import _Self.buildTypes.CompilerDistAndMavenArtifacts
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object BootstrapTest_LINUX : BuildType({
    name = "🐧 Test Compiler Bootstrapping K1"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
        **/build/reports/tests/**=>internal/test_results.zip
        **/build/test-results/**=>internal/test_results.zip
        **/build.log=>internal/test_results.zip
        build/internal/repo => internal/repo
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("gradleParameters", "%globalGradleParameters% -PlanguageVersion=1.9 -Pkotlin.native.enabled=true -PdeployVersion=%DeployVersion% -Pbuild.number=%build.number% -Pbootstrap.local=true -Pbootstrap.local.version=${CompilerDistAndMavenArtifacts.depParamRefs["DeployVersion"]} -Pbootstrap.local.path=%teamcity.build.checkoutDir%/bootstrap")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("bootstrapTestTasks", "coreLibsTest testsForBootstrapBuildTest")
        param("build.number.default", "${CompilerDistAndMavenArtifacts.depParamRefs.buildNumber}-bootstrap")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Compile All Kotlin"
            tasks = "compileKotlin"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Compile Tests in Kotlin"
            tasks = "compileTestKotlin"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Compile everything before running tests"
            tasks = "%bootstrapTestTasks%"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel --continue -Pkotlin.build.disable.verification.tasks=true -Dscan.tag.verification-tasks-disabled"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Bootstrap tests"
            tasks = "%bootstrapTestTasks%"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel --continue"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 100
    }

    features {
        swabra {
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
        dependency(_Self.buildTypes.CompilerDistAndMavenArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "maven.zip!** => bootstrap"
            }
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
