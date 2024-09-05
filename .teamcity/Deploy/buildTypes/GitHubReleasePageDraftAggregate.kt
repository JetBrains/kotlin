package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon

object GitHubReleasePageDraftAggregate : BuildType({
    name = "🦄 GitHub Release Page Draft Aggregate"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    features {
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
        snapshot(DeployCompilerArtifactsToGitHub) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(DeployReleasePageDraftToGitHub) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(PublishMavenArtifactsToGitHub) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }
})
