package Service.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object CompilerDiagnosticsIntroductionNotifier : BuildType({
    name = "🐧 Compiler diagnostics introduction notifier"

    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 10
            triggerRules = """
                +:kotlin/compiler/frontend/src/org/jetbrains/kotlin/diagnostics/Errors.java
                +:kotlin/compiler/frontend.java/src/org/jetbrains/kotlin/resolve/jvm/diagnostics/ErrorsJvm.java
                +:kotlin/js/js.frontend/src/org/jetbrains/kotlin/js/resolve/diagnostics/ErrorsJs.java
                +:kotlin/native/frontend/src/org/jetbrains/kotlin/resolve/konan/diagnostics/ErrorsNative.kt
            """.trimIndent()
            branchFilter = "+:<default>"
            enableQueueOptimization = false
        }
    }

    features {
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "#kotlin-compiler-qa-internal"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            buildStarted = true
            buildFailed = false
            firstFailureAfterSuccess = true
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
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
