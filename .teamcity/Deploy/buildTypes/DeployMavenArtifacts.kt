package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon

object DeployMavenArtifacts : BuildType({
    name = "🐧 Deploy Maven Artifacts"
    description = "Deploys Kotlin and Kotlin/Native artifacts to a Maven repository"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        select("reverse.dep.*.deploy-url", "file://%teamcity.build.checkoutDir%/kotlin/build/local-publish", label = "URL", display = ParameterDisplay.PROMPT,
                options = listOf("kotlin.space (kotlin dev)" to "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev", "kotlin.space (kotlin bootstrap)" to "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap", "kotlin.space (kotlin staging)" to "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/staging", "wasm experimental at kotlin.jetbrains.space (https://kotlin.jetbrains.space/p/wasm/packages/maven/experimental)" to "https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental", "kotlin-ide-plugin-dependencies at kotlin.jetbrains.space" to "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies"))
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
