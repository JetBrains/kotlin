package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object DeployUserProjectsArtifacts : BuildType({
    name = "🐧 Deploy User Projects Artifacts"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("publishing-util-version", "0.1.98")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        script {
            name = "Upload Artifacts"
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                ./space-cli/bin/space-cli \
                    upload \
                        --url https://kotlin.jetbrains.space \
                        --username %space.kotlin.packages.user% \
                        --password %space.kotlin.packages.secret% \
                        --project KOTLIN \
                        --repo experimental \
                        --dir repo \
                        --versions "${BuildNumber.depParamRefs["deployVersion"]}"
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 30
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
        dependency(_Self.buildTypes.Artifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = "+:maven.zip!**=>repo/${BuildNumber.depParamRefs["deployVersion"]}"
            }
        }
        artifacts(AbsoluteId("Kotlin_ServiceTasks_BintrayUtils_Build")) {
            buildRule = build("%publishing-util-version%")
            artifactRules = "space-cli-%publishing-util-version%.zip!/space-cli-%publishing-util-version%/**=>space-cli"
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-deployment")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
