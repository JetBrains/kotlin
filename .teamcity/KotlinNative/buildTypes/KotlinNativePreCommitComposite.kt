package KotlinNative.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object KotlinNativePreCommitComposite : BuildType({
    name = "Aggregate Native pre-commit builds (rrn/*)"
    description = "Pre-commit testing"

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
            branchFilter = """
                +:rrn/*
                -:rrn/perf/*
            """.trimIndent()
        }
        vcs {
            enabled = false
            triggerRules = """
                +:kotlin-native/**
                +:native/**
                -:native/native.tests/tests-gen/**
            """.trimIndent()
            branchFilter = "+:rr/*"
        }
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
            triggerRules = "-:ChangeLog.md"
            branchFilter = """
                +:<default>
                -:*
            """.trimIndent()
            perCheckinTriggering = true
            groupCheckinsByCommitter = true
        }
    }

    features {
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "#kotlin-native-build-notifications"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            firstFailureAfterSuccess = true
            firstSuccessAfterFailure = true
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativeRuntimePreCommitTests) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerUnitTest_linux_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerUnitTest_macos_arm64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerUnitTest_mingw_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Without_Stdlib_linux_x64_bundle_cM_n_gS_ag_uTSC_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_cM_e_gT_c_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_cM_f_gT_p_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_cM_n_a_s) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_cM_n_gS_ag_uTSC_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_gT_c_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_m_1) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_oM_o_cM_n_gT_p_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_cM_e_gT_p_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_cM_f_gT_c_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_gT_p_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_oM_o_cM_n_gT_c_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_oM_o_cM_n_gT_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_mingw_x64_bundle_cM_n_gT_c_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_mingw_x64_bundle_cM_n_gT_p_gS_ad_a_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_ios_simulator_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_linux_x64_bundle_gT_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_linux_x64_bundle_gT_s) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_mingw_x64_bundle_cM_n_gT_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_mingw_x64_bundle_cM_n_gT_s) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
    }
})
