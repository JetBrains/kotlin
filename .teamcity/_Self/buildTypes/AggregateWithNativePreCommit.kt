package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object AggregateWithNativePreCommit : BuildType({
    name = "Aggregate With Native Pre-Commit"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot)
        root(_Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master)
    }

    triggers {
        vcs {
            enabled = false
            triggerRules = "-:ChangeLog.md"
            branchFilter = "+:<default>"
        }
    }

    dependencies {
        snapshot(Aggregate) {
        }
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNative.buildTypes.KotlinNativePreCommitComposite) {
        }
    }
})
