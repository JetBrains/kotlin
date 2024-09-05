package Tests_Windows

import Tests_Windows.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("Tests_Windows")
    name = "Tests (Windows)"

    buildType(GradleIntegrationTestsGradleKotlinJStests_WINDOWS)
    buildType(JsTestsNightlyWindowsAggregate)
    buildType(JSCompilerTests_WINDOWS)
    buildType(JSCompilerTestsFIRES6_WINDOWS)
    buildType(JVMCompilerTests_WINDOWS)
    buildType(WASMCompilerTests_WINDOWS)
    buildType(GradleIntegrationTestsOtherGradleKotlintests_WINDOWS)
    buildType(KAPTCompilerTests_WINDOWS)
    buildType(JSCompilerTestsFIR_WINDOWS)
    buildType(JSCompilerTestsIRES6_WINDOWS)
    buildType(GradleIntegrationTestsAndroidKGPtests_WINDOWS)
    buildType(GradleIntegrationTestsJVM_WINDOWS)
    buildType(GenerateTests_WINDOWS)
    buildType(GradleIntegrationTestsGradleKotlinMPPtests_WINDOWS)
    buildType(TestBuildingKotlinWithCache_Windows)
    buildType(GradleIntegrationTestsGradleKotlinnativetests_WINDOWS)
    buildType(MiscTests_WINDOWS)
    buildType(JSCompilerTestsIR_WINDOWS)
    buildType(MiscCompilerTests_WINDOWS)
    buildType(WASMCompilerTestsK2_WINDOWS)
    buildType(GradleIntegrationTestsGradleandKotlindaemonsKGPtests_WINDOWS)
    buildType(ParcelizeTests_WINDOWS)
    buildType(GradleIntegrationTestsNightlyAggregate)
})
