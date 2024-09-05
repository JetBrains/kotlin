package KotlinNative.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object KotlinNativeLatestStableXcodeComposite : BuildType({
    name = "Latest Stable Xcode tests (Native, composite)"
    description = "Xcode latest stable tests run on VM agents"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("reverse.dep.*.env.KONAN_USE_INTERNAL_SERVER", "0")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("reverse.dep.Kotlin_BuildPlayground_Aquarius_KotlinNativeDist*.env.KONAN_USE_INTERNAL_SERVER", "1")
        param("konanMetaVersion", "%build.number.native.meta.version%")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot)

        showDependenciesChanges = true
    }

    triggers {
        schedule {
            enabled = false
            schedulingPolicy = weekly {
                hour = 0
            }
            branchFilter = """
                +:<default>
                -:*
            """.trimIndent()
            triggerBuild = always()
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
            buildFailed = true
            buildFinishedSuccessfully = true
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeRuntime_Tests_macos_arm64_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeSamples_macos_arm64_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeiOS_Upload_Test_macos_arm64_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeCompilerUnitTest_macos_arm64_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_ios_arm64_bundle_cM_e_cO_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle__stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_cM_e_gT_p_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_cM_f_gT_s_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_cM_f_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_m_1_cM_f_gT_c_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_m_1_gT_s_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_oM_n_cM_n_gT_c_gS_ag_a_m_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_oM_o_cM_n_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_macos_arm64_bundle__stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_cM_e_gT_c_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_cM_f_gT_n_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_cM_n_gT_n_gS_ag_a_s_uTSC_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_gT_c_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_gT_n_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_oM_n_cM_n_gT_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_tvos_arm64_bundle_oM_o_cM_n_gT_s_gS_ag_a_m_cO_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_tvos_simulator_arm64_bundle_cM_n_gT_p_gS_ag_a_s_uTSC_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_tvos_simulator_arm64_bundle_cM_n_gT_s_gS_ad_a_c_uTSC_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_tvos_simulator_arm64_bundle_oM_n_cM_n_gT_n_gS_ad_a_m_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_tvos_simulator_arm64_bundle_oM_o_cM_n_gT_c_gS_m_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_watchos_simulator_arm64_bundle_cM_n_gT_p_gS_m_a_m_uTSC_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_watchos_simulator_arm64_bundle_m_1_oM_o_cM_n_gT_n_gS_ad_a_s_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_K2_watchos_simulator_arm64_bundle_oM_n_cM_n_gT_s_gS_ag_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_ios_simulator_arm64_bundle_cM_n_a_m_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_ios_simulator_arm64_bundle_cM_n_gT_s_gS_m_a_s_uTSC_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_ios_simulator_arm64_bundle_cM_n_uTSC_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_ios_simulator_arm64_bundle_m_1_cM_n_gT_n_gS_m_a_m_uTSC_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_ios_simulator_arm64_bundle_oM_o_cM_n_gT_s_gS_ag_a_m_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_cM_e_gT_n_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_cM_f_gT_p_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_cM_n_gT_s_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_gT_p_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_m_1_cM_e_gT_s_gS_ad_a_c_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_m_1_oM_o_cM_n_gT_p_gS_m_a_m_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_oM_n_cM_n_gT_p_gS_ad_a_m_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_tvos_simulator_arm64_bundle_m_1_cM_n_gT_c_gS_ag_a_s_uTSC_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_watchos_arm64_bundle_cM_n_gT_p_uTSC_e_cO_e_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeLatestXcodeTests.buildTypes.NativeTest_Without_K2_watchos_simulator_arm64_bundle_m_1_oM_n_cM_n_gT_c_gS_m_a_s_stable) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
    }
})
