package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.triggers.retryBuild

object KotlinNativePublish : BuildType({
    name = "🐧 Publish Kotlin/Native"
    description = "Kotlin/Native distribution publish composite (deploy)"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("konanMetaVersion", "%build.number.native.meta.version%")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
    }

    triggers {
        retryBuild {
            attempts = 2
            branchFilter = "+:master"
        }
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
        snapshot(KotlinNativePublishMaven) {
        }
        snapshot(KotlinNativePublishToS3) {
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
