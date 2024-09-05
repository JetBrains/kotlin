package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import _Self.buildTypes.CompilerArtifacts
import _Self.buildTypes.CompilerArtifactsInDocker
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object TestReproducibleCompilerDockerVsAgent : BuildType({
    name = "🐧 Test compiler build is reproducible (on agent / in docker)"

    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("equality-exceptions", "")
        param("trusted-binaries-equivalence-checker-version", "0.1.5")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        script {
            name = "Check artifacts Kotlin_BuildPlayground_Aquarius_CompilerArtifactsInDocker <-> Kotlin_BuildPlayground_Aquarius_CompilerArtifacts"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            scriptContent = """
                #!/bin/bash
                set -x
                export JAVA_HOME=%env.JDK_11_0%
                ./trusted-binaries-equivalence-checker/bin/trusted-binaries-equivalence-checker   "Kotlin_BuildPlayground_Aquarius_CompilerArtifactsInDocker/${CompilerArtifactsInDocker.depParamRefs["kotlin.compiler.zip.name"]}" "Kotlin_BuildPlayground_Aquarius_CompilerArtifacts/${CompilerArtifacts.depParamRefs["kotlin.compiler.zip.name"]}"
            """.trimIndent()
            formatStderrAsError = true
        }
    }

    failureConditions {
        executionTimeoutMin = 20
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
        dependency(_Self.buildTypes.CompilerArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "+:${CompilerArtifacts.depParamRefs["kotlin.compiler.zip.name"]}=>Kotlin_BuildPlayground_Aquarius_CompilerArtifacts"
            }
        }
        dependency(_Self.buildTypes.CompilerArtifactsInDocker) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "+:${CompilerArtifactsInDocker.depParamRefs["kotlin.compiler.zip.name"]}=>Kotlin_BuildPlayground_Aquarius_CompilerArtifactsInDocker"
            }
        }
        artifacts(AbsoluteId("Kotlin_ServiceTasks_TrustedBinariesEquivalenceChecker")) {
            buildRule = build("%trusted-binaries-equivalence-checker-version%")
            artifactRules = "trusted-binaries-equivalence-checker-%trusted-binaries-equivalence-checker-version%.zip!/trusted-binaries-equivalence-checker-%trusted-binaries-equivalence-checker-version%/**=>trusted-binaries-equivalence-checker"
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
