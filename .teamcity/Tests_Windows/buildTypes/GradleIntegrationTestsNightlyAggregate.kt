package Tests_Windows.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications

object GradleIntegrationTestsNightlyAggregate : BuildType({
    name = "Gradle Integration Tests Nightly Aggregate"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

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
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(GradleIntegrationTestsAndroidKGPtests_WINDOWS) {
        }
        snapshot(GradleIntegrationTestsGradleKotlinJStests_WINDOWS) {
        }
        snapshot(GradleIntegrationTestsGradleKotlinMPPtests_WINDOWS) {
        }
        snapshot(GradleIntegrationTestsGradleKotlinnativetests_WINDOWS) {
        }
        snapshot(GradleIntegrationTestsGradleandKotlindaemonsKGPtests_WINDOWS) {
        }
        snapshot(GradleIntegrationTestsOtherGradleKotlintests_WINDOWS) {
        }
    }
})
