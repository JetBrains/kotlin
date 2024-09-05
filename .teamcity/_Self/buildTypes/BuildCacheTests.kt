package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object BuildCacheTests : BuildType({
    name = "Build cache tests"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    triggers {
        vcs {
            enabled = false
            branchFilter = "+:<default>"
        }
    }

    dependencies {
        snapshot(BuildKotlinToDirectoryCache) {
        }
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(Tests_Linux.buildTypes.TestBuildingKotlinWithCache_Linux) {
        }
        snapshot(Tests_Linux.buildTypes.TestBuildingKotlinWithCache_Linux_RemoteCache) {
        }
        snapshot(Tests_Windows.buildTypes.TestBuildingKotlinWithCache_Windows) {
        }
    }
})
