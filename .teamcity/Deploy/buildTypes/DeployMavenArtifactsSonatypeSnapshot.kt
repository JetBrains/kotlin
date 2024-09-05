package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object DeployMavenArtifactsSonatypeSnapshot : BuildType({
    name = "🐧 Maven Artifacts (Sonatype SNAPSHOT)"
    description = "Automatically deploys every successful build to sonatype"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("reverse.dep.*.deploy-url", "")
        password("reverse.dep.*.system.kotlin.sonatype.password", "credentialsJSON:81999f5f-6084-4108-ab95-4d4ad6bd57c4", display = ParameterDisplay.HIDDEN)
        password("reverse.dep.*.system.kotlin.sonatype.user", "credentialsJSON:688681d2-8b46-4ba9-b9e3-d3cc78184b54", display = ParameterDisplay.HIDDEN)
        param("reverse.dep.*.deploy-repo", "sonatype-nexus-snapshots")
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 5400
            branchFilter = "+:<default>"
        }
    }

    features {
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "#kotlin-bots"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            buildFailed = true
            firstFailureAfterSuccess = true
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
        snapshot(DeployKotlinMavenArtifacts) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativePublishMaven) {
            onDependencyFailure = FailureAction.FAIL_TO_START
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
