package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import _Self.buildTypes.CompilerDistAndMavenArtifacts
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object PublishToNpm : BuildType({
    name = "🐧 Publish to NPM"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${CompilerDistAndMavenArtifacts.depParamRefs.buildNumber}"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        param("system.kotlin.npmjs.auth.token", "%kotlin.npmjs.auth.token%")
        text("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}", display = ParameterDisplay.PROMPT)
        param("system.kotlin.deploy.version", "%DeployVersion%")
        text("system.kotlin.deploy.tag", "dev", description = """if ReleaseStatus is RELEASE tag with "latest" else "dev"""", display = ParameterDisplay.PROMPT)
        param("tasks", "publishAll")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Publish to NPM"
            tasks = "%tasks%"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -p js/npm --no-scan --info --stacktrace"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    features {
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        dependency(_Self.buildTypes.CompilerDistAndMavenArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = "kotlin-compiler-*.zip!/kotlinc=>kotlin/dist/kotlinc"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-deployment")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
