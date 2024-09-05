package Tests_MacOS

import Tests_MacOS.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("Tests_MacOS")
    name = "Tests (MacOS)"

    buildType(GradleIntegrationTestsNativeMacarm64_MACOS)
    buildType(KotlinGradlePluginFunctionalTestsMacOS_RemoteRun)
    buildType(GradleIntegrationTestsNativeMacx64_MACOS)
    buildType(GradleIntegrationTestsNativeMacSwiftExportarm64_MACOS)
    buildType(PrivacyManifestsPluginTests)
    buildType(SwiftExportEmbeddableValidations)
    buildType(KotlinGradlePluginFunctionalTestsMacOS_Nightly)
})
