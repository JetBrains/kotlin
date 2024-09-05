package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object Nightly : BuildType({
    name = "Nightly"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    triggers {
        schedule {
            enabled = false
            schedulingPolicy = daily {
                hour = 1
            }
            branchFilter = "+:<default>"
            triggerBuild = onWatchedBuildChange {
                buildType = "${BuildNumber.id}"
                watchedBuildRule = ScheduleTrigger.WatchedBuildRule.LAST_SUCCESSFUL
            }
            withPendingChangesOnly = false
        }
    }

    dependencies {
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNative.buildTypes.KotlinNativeNightlyComposite) {
        }
        snapshot(LibraryReferenceLatestDocs) {
        }
        snapshot(LibraryReferenceLegacyDocs) {
        }
        snapshot(NightlyCritical) {
        }
        snapshot(Tests_MacOS.buildTypes.PrivacyManifestsPluginTests) {
        }
        snapshot(Tests_MacOS.buildTypes.SwiftExportEmbeddableValidations) {
        }
    }
})
