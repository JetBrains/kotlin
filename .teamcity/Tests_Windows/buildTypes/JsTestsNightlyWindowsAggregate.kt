package Tests_Windows.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications

object JsTestsNightlyWindowsAggregate : BuildType({
    name = "JS Tests Nightly (Windows)"

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
        snapshot(Tests_Linux.buildTypes.FIRCompilerNightlyTests_LINUX) {
        }
        snapshot(JSCompilerTestsFIRES6_WINDOWS) {
        }
        snapshot(JSCompilerTestsFIR_WINDOWS) {
        }
        snapshot(JSCompilerTestsIRES6_WINDOWS) {
        }
        snapshot(JSCompilerTests_WINDOWS) {
        }
        snapshot(WASMCompilerTests_WINDOWS) {
        }
    }
})
