package KotlinNativePerformanceTests

import KotlinNativePerformanceTests.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("KotlinNativePerformanceTests")
    name = "Performance"

    buildType(KotlinNativePerformance_Tests_New_MM_Pre_commit_macos_x64)
    buildType(KotlinNativePerformance_Tests_New_MM_Pre_commit_mingw_x64)
    buildType(KotlinNativePerformance_Tests_New_MM_Pre_commit_macos_arm64)
    buildType(KotlinNativePerformance_Tests_Debug_macos_arm64)
    buildType(KotlinNativePerformance_Tests_Debug_macos_x64)
    buildType(KotlinNativePerformance_Tests_Debug_linux_x64)
    buildType(KotlinNativePerformance_Tests_Debug_mingw_x64)
    buildType(KotlinNativePerformance_Tests_New_MM_macos_arm64)
    buildType(KotlinNativePerformance_Tests_New_MM_linux_x64)
    buildType(KotlinNativePerformance_Tests_New_MM_Pre_commit_linux_x64)
    buildType(KotlinNativePerformance_Tests_New_MM_mingw_x64)
    buildType(KotlinNativePerformance_Tests_New_MM_macos_x64)
})
