package UserProjectCompiling.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object UserProjectsAggregateK1 : BuildType({
    name = "K1 All Projects (artifacts from TC)"

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
        }
    }

    dependencies {
        snapshot(BenchmarkCompilationCheckK1) {
        }
        snapshot(SerializationCompilationCheckK1) {
        }
        snapshot(ktor) {
        }
    }
})
