package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications

object NightlyCritical : BuildType({
    name = "Nightly (Monitored)"
    description = """
        Configurations where responsible team is committed to fix failures ASAP.
        Gradle Integration Tests Nightly Aggregate: Kotlin build tool team
        JS Tests Nightly (Windows): Kotlin JS team
        Maven Artifacts to kotlin.space (kotlin dev) (NIGHTLY): Kotlin Infrastructure team
        Test compiler build is reproducible (on agent / in docker): Kotlin Infrastructure team
        Test maven build is reproducible (on agent / in docker): Kotlin Infrastructure team
        Test maven build is reproducible when Gradle caches are enabled (in docker + no caches / on agent + caches): Kotlin Infrastructure team
        Test published maven artifacts are reproducible (on agent / in docker): Kotlin Infrastructure team
        Compiler Tests (21 JDK): Kotlin Compiler team,
        Aggregate Native pre-commit builds: Kotlin Native team
    """.trimIndent()

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
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
            firstFailureAfterSuccess = true
        }
    }

    dependencies {
        snapshot(Aggregate) {
        }
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(Deploy.buildTypes.DeployMavenArtifacts_Nightly_kotlin_space_packages) {
        }
        snapshot(Tests_Windows.buildTypes.GradleIntegrationTestsNightlyAggregate) {
        }
        snapshot(Tests_Windows.buildTypes.JsTestsNightlyWindowsAggregate) {
        }
        snapshot(KotlinNative.buildTypes.KotlinNativePreCommitComposite) {
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
        snapshot(Deploy.buildTypes.PublishToGradlePluginPortalValidate) {
        }
        snapshot(PublishToNpmDryRun) {
        }
        snapshot(Deploy.buildTypes.RelayNightlyDevToSonatype) {
        }
        snapshot(Tests_Linux.buildTypes.TestReproducibleCompilerDockerVsAgent) {
        }
        snapshot(Tests_Linux.buildTypes.TestReproducibleDeployMavenVsDocker) {
        }
        snapshot(Tests_Linux.buildTypes.TestReproducibleMavenAgentVsDocker) {
        }
        snapshot(Tests_Linux.buildTypes.TestReproducibleMavenDockerVsAgentAndCaches) {
        }
    }
})
