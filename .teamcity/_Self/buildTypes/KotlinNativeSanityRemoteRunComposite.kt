package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*

object KotlinNativeSanityRemoteRunComposite : BuildType({
    name = "Aggregate Native sanity"
    description = "Kotlin/Native Sanity Remote run (rr/-branch) testing"

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

    dependencies {
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(KotlinNativeTests.buildTypes.KotlinNativeSir_Tests_linux_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerUnitTest_linux_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeCompilerUnitTest_mingw_x64) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_linux_x64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_K2_macos_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_linux_x64_bundle_m_1) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_Without_K2_macos_arm64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_linux_x64_bundle_) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(KotlinNativeTests.buildTypes.NativeTest_linux_x64_bundle_cM_e) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
    }
})
