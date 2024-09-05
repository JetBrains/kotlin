package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Aggregate : BuildType({
    name = "Aggregate (master)"
    description = "Run build with the [Aligner for Aggregate](https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_BuildPlayground_Aquarius_MainAggregateAligner)"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("teamcity.ui.runButton.caption", "Run (No IJ Aligner)")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(_Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master, "+:. => intellij-kt-master")

        checkoutMode = CheckoutMode.MANUAL
    }

    triggers {
        vcs {
            enabled = false
            triggerRules = "-:ChangeLog.md"
            branchFilter = "+:<default>"
        }
    }

    features {
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "#kotlin-bots"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            buildFailed = true
            newBuildProblemOccurred = true
            buildFinishedSuccessfully = true
            firstSuccessAfterFailure = true
        }
    }

    dependencies {
        snapshot(Tests_Linux.buildTypes.AndroidCodegenTests_LINUX) {
        }
        dependency(Artifacts) {
            snapshot {
            }

            artifacts {
                artifactRules = "+:**/*"
            }
        }
        snapshot(Tests_Linux.buildTypes.ArtifactsTests) {
        }
        snapshot(Tests_Linux.buildTypes.BenchmarkCompilationCheckK2) {
        }
        snapshot(Tests_Linux.buildTypes.BootstrapTestFir_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.BootstrapTest_LINUX) {
        }
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(Tests_Linux.buildTypes.CheckBuildTest_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.CodebaseTests) {
        }
        snapshot(Tests_Linux.buildTypes.CodegenTestsOnJDK11_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.CodegenTestsOnJDK17_LINUX) {
        }
        snapshot(CompileAllClasses) {
        }
        snapshot(CompilerDist) {
        }
        snapshot(CompilerDistLocal) {
        }
        snapshot(CompilerDistLocalK2) {
        }
        snapshot(CompilerDistLocalOverrideObsoleteJdk) {
        }
        snapshot(Tests_Linux.buildTypes.ComposeTests) {
        }
        snapshot(Tests_Linux.buildTypes.FIRCompilerTests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.FrontendApiTests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.GenerateTests_LINUX) {
        }
        snapshot(Tests_Windows.buildTypes.GenerateTests_WINDOWS) {
        }
        snapshot(Tests_Linux.buildTypes.GradleIntegrationTestsAndroidKGPtests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.GradleIntegrationTestsGradleKotlinJStests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.GradleIntegrationTestsGradleKotlinMPPtests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.GradleIntegrationTestsGradleKotlinnativetests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.GradleIntegrationTestsGradleandKotlindaemonsKGPtests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.GradleIntegrationTestsJVM_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.GradleIntegrationTestsOtherGradleKotlintests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.IrBackendCommonTests) {
        }
        snapshot(Tests_Linux.buildTypes.JPS_Tests) {
        }
        snapshot(Tests_Linux.buildTypes.JSCompilerTestsFIRES6_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.JSCompilerTestsFIR_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.JSCompilerTestsIRES6_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.JSCompilerTestsIR_LINUX) {
        }
        snapshot(Tests_Windows.buildTypes.JSCompilerTestsIR_WINDOWS) {
        }
        snapshot(Tests_Linux.buildTypes.JSCompilerTests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.JVMCompilerTests_LINUX) {
        }
        snapshot(Tests_Windows.buildTypes.JVMCompilerTests_WINDOWS) {
        }
        snapshot(Tests_Linux.buildTypes.KAPTCompilerTests_LINUX) {
        }
        snapshot(Tests_Windows.buildTypes.KAPTCompilerTests_WINDOWS) {
        }
        snapshot(KotlinNativeSanityRemoteRunComposite) {
        }
        snapshot(KotlinxLibrariesCompilation) {
        }
        snapshot(Tests_Linux.buildTypes.MiscCompilerTests_LINUX) {
        }
        snapshot(Tests_Windows.buildTypes.MiscCompilerTests_WINDOWS) {
        }
        snapshot(Tests_Linux.buildTypes.MiscTests_LINUX) {
        }
        snapshot(Tests_Windows.buildTypes.MiscTests_WINDOWS) {
        }
        snapshot(Tests_Linux.buildTypes.ParcelizeTests_LINUX) {
        }
        snapshot(Tests_Windows.buildTypes.ParcelizeTests_WINDOWS) {
        }
        snapshot(ResolveDependencies) {
        }
        snapshot(Tests_Linux.buildTypes.SerializationCompilationCheckK2) {
        }
        snapshot(Tests_Linux.buildTypes.StatisticsPluginTests) {
        }
        snapshot(Tests_Linux.buildTypes.TestBuildingKotlinWithCache_Linux) {
        }
        snapshot(Tests_Windows.buildTypes.TestBuildingKotlinWithCache_Windows) {
        }
        snapshot(ValidateIdePluginDependencies) {
        }
        snapshot(Tests_Linux.buildTypes.WASMCompilerTestsK2_LINUX) {
        }
        snapshot(Tests_Windows.buildTypes.WASMCompilerTestsK2_WINDOWS) {
        }
        snapshot(Tests_Linux.buildTypes.WASMCompilerTests_LINUX) {
        }
        snapshot(Tests_Linux.buildTypes.kotlinxAtomicfu) {
        }
        snapshot(Tests_Linux.buildTypes.kotlinxAtomicfuK2Aggregate) {
        }
        snapshot(Tests_Linux.buildTypes.kotlinxcoroutines) {
        }
        snapshot(AbsoluteId("ijplatform_master_KotlinCompileIncLatestCompiler_master")) {
        }
        snapshot(AbsoluteId("ijplatform_master_KotlinIdeaFirTestsLatestCompiler_master")) {
        }
        snapshot(AbsoluteId("ijplatform_master_KotlinTestsLatestCompiler_master")) {
        }
    }
})
