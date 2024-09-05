package KotlinNative.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object KotlinNativeWeeklyComposite : BuildType({
    name = "Aggregate Native weekly builds (master)"
    description = "Weekly testing, including compile-only tests"

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
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_Without_PL_ios_arm64_bundle_oM_o_cM_n_sTE_e_eGC_e_x_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_tvos_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_tvos_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_tvos_simulator_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_tvos_simulator_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_tvos_simulator_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_tvos_x64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_tvos_x64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_watchos_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_watchos_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_watchos_simulator_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_watchos_simulator_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_watchos_x64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_watchos_x64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_linux_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_linux_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_tvos_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_tvos_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_tvos_simulator_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_tvos_simulator_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_tvos_x64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_tvos_x64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_watchos_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_watchos_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_watchos_simulator_arm64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_watchos_simulator_arm64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_watchos_x64_bundle_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_watchos_x64_bundle_oM_o_cM_n_cO_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
    }
})
