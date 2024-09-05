package KotlinNative.buildTypes

import Deploy.buildTypes.KotlinNativePublish
import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

object KotlinNativePerformanceTests_1 : BuildType({
    id("KotlinNativePerformanceTests")
    name = "Performance Tests (Native, composite)"
    description = "Performance tests composite configuration"

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
        finishBuildTrigger {
            enabled = false
            buildType = "${KotlinNativePublish.id}"
            successfulOnly = true
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativePerformanceTests.buildTypes.KotlinNativePerformance_Tests_Debug_linux_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePerformanceTests.buildTypes.KotlinNativePerformance_Tests_Debug_macos_arm64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePerformanceTests.buildTypes.KotlinNativePerformance_Tests_Debug_macos_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePerformanceTests.buildTypes.KotlinNativePerformance_Tests_Debug_mingw_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePerformanceTests.buildTypes.KotlinNativePerformance_Tests_New_MM_linux_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePerformanceTests.buildTypes.KotlinNativePerformance_Tests_New_MM_macos_arm64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePerformanceTests.buildTypes.KotlinNativePerformance_Tests_New_MM_macos_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativePerformanceTests.buildTypes.KotlinNativePerformance_Tests_New_MM_mingw_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(Deploy.buildTypes.KotlinNativePublish) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }
})
