package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object DeployGradlePluginIdea : BuildType({
    name = "🐧 Deploy Kotlin Gradle Plugin Idea artifacts"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        text("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}", display = ParameterDisplay.PROMPT)
        param("deploy-repo", "kotlin-space-packages")
        param("deploy-url", "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        password("system.kotlin.kotlin-space-packages.user", "credentialsJSON:70dd5a56-00b7-43a9-a2b7-e9c802b5d17d", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        password("system.kotlin.kotlin-space-packages.password", "credentialsJSON:6a448008-2992-4f63-9a64-2e5013887a2e", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Publish Gradle Plugin IDEA artifacts"
            tasks = ":kotlin-tooling-core:publish :kotlin-gradle-plugin-annotations:publish :kotlin-gradle-plugin-idea:publish :kotlin-gradle-plugin-idea-proto:publish"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -PdeployVersion=%DeployVersion% -Pdeploy-repo=%deploy-repo% -Pdeploy-url=%deploy-url% --no-scan --info --stacktrace"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 90
        errorMessage = true
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
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
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
