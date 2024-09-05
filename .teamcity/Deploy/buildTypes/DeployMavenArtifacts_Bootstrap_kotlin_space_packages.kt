package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon

object DeployMavenArtifacts_Bootstrap_kotlin_space_packages : BuildType({
    name = "🐧 Maven Artifacts to kotlin.space (kotlin bootstrap) (BOOTSTRAP)"
    description = "Automatically deploys build tagged with 'bootstrap' to kotlin.space (kotlin bootstrap)"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("reverse.dep.Kotlin_BuildPlayground_Aquarius_DeployKotlinMavenArtifacts.deploy-url", "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        param("reverse.dep.Kotlin_BuildPlayground_Aquarius_KotlinNativePublishMaven.deploy-url", "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        text("reverse.dep.*.deploy-repo", "kotlin-space-packages")
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
        snapshot(DeployIdePluginDependenciesMavenArtifacts) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(DeployKotlinMavenArtifacts) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativePublishMaven) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-deployment")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
