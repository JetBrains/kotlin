package KotlinNative

import KotlinNative.buildTypes.*
import KotlinNative.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("KotlinNative")
    name = "Kotlin/Native"

    vcsRoot(Kotlin_Native_IOS_Upload_Test)

    buildType(KotlinNativePerformanceTests_1)
    buildType(KotlinNativeDist_macos_arm64_BUNDLE)
    buildType(KotlinNativeGradleSamples)
    buildType(KotlinNativeDist_linux_x64_LIGHT_BUNDLE)
    buildType(KotlinNativeLatestCustomXcodeComposite)
    buildType(KotlinNativeNightlyComposite)
    buildType(KotlinNativePreCommitComposite)
    buildType(KotlinNativeLatestStableXcodeComposite)
    buildType(KotlinNativeRuntimePreCommitTests)
    buildType(KotlinNativeWeeklyComposite)
    buildType(KotlinNativePerformancePreCommitTests)
    buildType(KotlinNativeRuntimeTests)
    buildType(KotlinNativeDist_macos_x64_BUNDLE)
    buildType(KotlinNativeDist_mingw_x64_BUNDLE)
    buildType(KotlinNativeLatestBetaXcodeComposite)
    buildType(KotlinNativeDist_linux_x64_BUNDLE)
    buildType(KotlinNativePreCommitPerformanceComposite)
    buildType(KotlinNativeDist_mingw_x64_LIGHT_BUNDLE)

    subProject(KotlinNativePerformanceTests.Project)
    subProject(KotlinNativeLatestXcodeTests.Project)
    subProject(KotlinNativeTests.Project)
})
