package XcodeUpdate.buildTypes

import jetbrains.buildServer.configs.kotlin.*

object CustomXcodeAggregate : BuildType({
    name = "Custom Xcode Tests Aggregate"
    description = """
        Aggregate build for testing Xcode releases and does the following: 
        - consumes as a parameter version of Xcode 
        - triggers build of image for iCRI virtual machines with specified Xcode versions
        - runs Kotlin Gradle Plugin Integration Tests on a specified Xcode version
        - runs K/N Compiler tests on a specified Xcode version
    """.trimIndent()

    type = BuildTypeSettings.Type.COMPOSITE

    params {
        param("reverse.dep.*.env.PUSH_TAG", "%env.XCODE_UNDER_TEST_VERSION%")
        text("reverse.dep.*.env.XCODE_UNDER_TEST_VERSION", "", label = "Xcode under test version", description = """
            Specifies concrete Xcode version, for a proper version number check 
            https://repo.labs.intellij.net/ui/native/xcode-distr/
        """.trimIndent(), display = ParameterDisplay.PROMPT,
              regex = """^\d.*""", validationMessage = "Invalid Xcode version, should start with digit, e.g. 15_Release_Candidate")
    }

    dependencies {
        snapshot(GradleIntegrationTestsLatestXcode_MACOS) {
        }
        snapshot(KotlinNative.buildTypes.KotlinNativeLatestCustomXcodeComposite) {
        }
    }
})
