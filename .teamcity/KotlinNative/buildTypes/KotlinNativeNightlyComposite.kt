package KotlinNative.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications

object KotlinNativeNightlyComposite : BuildType({
    name = "Aggregate Native nightly builds (master)"
    description = "Nightly testing"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("reverse.dep.*.kotlin.native.platformLibs.updateDefFileDependencies", "true")
        param("gradleParameters", "%globalGradleParameters%")
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
        param("konanMetaVersion", "%build.number.native.meta.version%")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot)

        showDependenciesChanges = true
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
        snapshot(KotlinNativeTests.buildTypes.CoroutinesBinaryCompatibilityTests_linux_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.CoroutinesBinaryCompatibilityTests_macos_arm64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeGradleSamples) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePerformanceTests_1) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(Deploy.buildTypes.KotlinNativePublish) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeRuntimeTests) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.KotlinNativeiOS_Upload_Test_macos_arm64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerUnitTest_linux_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerUnitTest_macos_arm64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerUnitTest_macos_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerUnitTest_mingw_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2OneStageAndK1_linux_x64_bundle_m_1) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2OneStageAndK1_macos_arm64_bundle_m_1) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2OneStage_mingw_x64_bundle_m_1_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Only_PL_macos_x64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Only_PL_macos_x64_bundle_cM_n_a_m) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Only_PL_macos_x64_bundle_oM_o_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Stdlib_ios_arm64_bundle_sTE_e_eGC_e_x_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Stdlib_macos_arm64_bundle_gT_c_sTE_e_eGC_e_x_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Without_PL_Tests_linux_x64_bundle_oM_o_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Without_PL_Tests_macos_arm64_bundle_cM_n_gS_ag_uTSC_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Without_PL_ios_simulator_arm64_bundle_oM_o_cM_n_sTE_e_eGC_e_x_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Without_PL_macos_arm64_bundle_oM_o_cM_n_gS_ag) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_ios_simulator_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_ios_x64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_cM_n_a_m) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_gT_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_gT_c_gS_ag) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_oM_o_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_oM_o_cM_n_gT_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_cM_n_a_m) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_gT_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_gT_p) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_gT_s) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_oM_o_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_oM_o_cM_n_gT_s) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_mingw_x64_bundle_cM_n_a_m) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_mingw_x64_bundle_gT_c) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_mingw_x64_bundle_oM_o_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_watchos_simulator_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Only_PL_macos_x64_bundle_cM_n_uTSC_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_Only_PL_macos_x64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_Only_PL_macos_x64_bundle_cM_n_a_m) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_Only_PL_macos_x64_bundle_oM_o_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_linux_x64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_linux_x64_bundle_cM_n_a_m) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_linux_x64_bundle_oM_o_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_cM_n_a_m) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_oM_o_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_mingw_x64_bundle_cM_n_a_m) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_mingw_x64_bundle_oM_o_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_ios_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_linux_x64_bundle_cM_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_linux_x64_bundle_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_macos_arm64_bundle_cM_n_gT_n_uTSC_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_mingw_x64_bundle_cM_n) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        artifacts(KotlinNativeDist_linux_x64_BUNDLE) {
            cleanDestination = true
            artifactRules = "kotlin-native-linux-x86_64-*.tar.gz=>bundle"
        }
        artifacts(KotlinNativeDist_macos_arm64_BUNDLE) {
            cleanDestination = true
            artifactRules = "kotlin-native-macos-aarch64-*.tar.gz=>bundle"
        }
        artifacts(KotlinNativeDist_macos_x64_BUNDLE) {
            cleanDestination = true
            artifactRules = "kotlin-native-macos-x86_64-*.tar.gz=>bundle"
        }
        artifacts(KotlinNativeDist_mingw_x64_BUNDLE) {
            cleanDestination = true
            artifactRules = "kotlin-native-windows-x86_64-*.zip=>bundle"
        }
    }
})
