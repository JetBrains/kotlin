package Tests_Linux.buildTypes

import Deploy.buildTypes.DeployKotlinMavenArtifacts
import _Self.buildTypes.BuildNumber
import _Self.buildTypes.MavenArtifactsDocker
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object TestReproducibleDeployMavenVsDocker : BuildType({
    name = "🐧 Test published maven artifacts are reproducible (on agent / in docker)"

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
            name = "Check artifacts Kotlin_BuildPlayground_Aquarius_DeployKotlinMavenArtifacts <-> Kotlin_BuildPlayground_Aquarius_MavenArtifactsDocker"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            scriptContent = """
                #!/bin/bash
                set -x
                export JAVA_HOME=%env.JDK_11_0%
                ./trusted-binaries-equivalence-checker/bin/trusted-binaries-equivalence-checker   "Kotlin_BuildPlayground_Aquarius_DeployKotlinMavenArtifacts/${DeployKotlinMavenArtifacts.depParamRefs["reproducible.maven.artifact"]}" "Kotlin_BuildPlayground_Aquarius_MavenArtifactsDocker/${MavenArtifactsDocker.depParamRefs["reproducible.maven.artifact"]}"
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
        dependency(Deploy.buildTypes.DeployKotlinMavenArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "+:${DeployKotlinMavenArtifacts.depParamRefs["reproducible.maven.artifact"]}=>Kotlin_BuildPlayground_Aquarius_DeployKotlinMavenArtifacts"
            }
        }
        dependency(_Self.buildTypes.MavenArtifactsDocker) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "+:${MavenArtifactsDocker.depParamRefs["reproducible.maven.artifact"]}=>Kotlin_BuildPlayground_Aquarius_MavenArtifactsDocker"
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
