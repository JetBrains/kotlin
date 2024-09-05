package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Quarantine : BuildType({
    name = "Quarantine"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(_Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master, "+:. => intellij-kt-master")

        checkoutMode = CheckoutMode.MANUAL
    }

    triggers {
        vcs {
            enabled = false
            triggerRules = "-:ChangeLog.md"
            branchFilter = "+:<default>"
        }
    }

    dependencies {
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(Tests_Windows.buildTypes.GradleIntegrationTestsJVM_WINDOWS) {
        }
    }
})
