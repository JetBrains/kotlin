package UserProjectsK2.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object UserProjectsAggregateK2 : BuildType({
    name = "K2 All Projects"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    triggers {
        vcs {
            enabled = false
            branchFilter = "+:<default>"
            perCheckinTriggering = true
            groupCheckinsByCommitter = true
        }
    }

    dependencies {
        snapshot(DokkaK2) {
        }
        snapshot(ExposedK2) {
        }
        snapshot(IntellijK2) {
        }
        snapshot(KotlinWasmTemplatesK2Aggregate) {
        }
        snapshot(KotlinxAtomicFuK2) {
        }
        snapshot(KotlinxBenchmarkK2) {
        }
        snapshot(KotlinxCollectionsImmutableK2) {
        }
        snapshot(KotlinxCoroutinesK2) {
        }
        snapshot(KotlinxDateTimeK2) {
        }
        snapshot(KtorK2) {
        }
        snapshot(SpaceAndroidK2) {
        }
        snapshot(SpaceK2) {
        }
        snapshot(SpaceiOSK2) {
        }
        snapshot(ToolboxEnterpriseK2) {
        }
        snapshot(_Self.buildTypes.TriggerUserProjectsTcc) {
        }
        snapshot(YouTrackK2) {
        }
    }
})
