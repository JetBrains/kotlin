package KotlinNative.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object KotlinNativePreCommitPerformanceComposite : BuildType({
    name = "Aggregate Native performance builds (rrn/perf/*)"
    description = "Pre-commit testing with Performance"

    type = BuildTypeSettings.Type.COMPOSITE
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

    vcs {
        root(DslContext.settingsRoot)

        showDependenciesChanges = true
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            branchFilter = "+:rrn/perf/*"
        }
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            triggerRules = "+:kotlin-native/**"
            branchFilter = """
                +:<default>
                -:*
            """.trimIndent()
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativePerformancePreCommitTests) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePreCommitComposite) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
    }
})
