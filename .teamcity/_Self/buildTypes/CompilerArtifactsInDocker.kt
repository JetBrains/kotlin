package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object CompilerArtifactsInDocker : BuildType({
    name = "🐧 Compiler Artifacts In Docker (no caches)"

    artifactRules = "kotlin_Kotlin_BuildPlayground_Aquarius_CompilerArtifactsInDocker/dist/kotlin-compiler-*"
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("docker.image", "kotlin.registry.jetbrains.space/p/kotlin/containers/kotlin-build-env:v8")
        param("gradleParameters", "%globalGradleParameters%")
        param("kotlin.compiler.zip.name", "kotlin-compiler-${BuildNumber.depParamRefs["deployVersion"]}.zip")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin_Kotlin_BuildPlayground_Aquarius_CompilerArtifactsInDocker")

        cleanCheckout = true
    }

    steps {
        script {
            name = "Build Kotlin Compiler"
            workingDir = "kotlin_Kotlin_BuildPlayground_Aquarius_CompilerArtifactsInDocker"
            scriptContent = "./scripts/build-kotlin-compiler.sh ${BuildNumber.depParamRefs["deployVersion"]} ${BuildNumber.depParamRefs.buildNumber}"
            dockerImage = "%docker.image%"
        }
    }

    failureConditions {
        executionTimeoutMin = 120
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3391"
            }
        }
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        exists("docker.version")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
