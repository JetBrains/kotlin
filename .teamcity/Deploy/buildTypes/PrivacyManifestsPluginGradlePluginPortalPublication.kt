package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object PrivacyManifestsPluginGradlePluginPortalPublication : BuildType({
    name = "🐧 Publish apple-privacy-manifests plugin to Gradle Plugin portal"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        password("system.gradle.publish.key", "credentialsJSON:08dfddc9-529c-48dd-95d1-68c376e53c9e", display = ParameterDisplay.HIDDEN)
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        password("system.gradle.publish.secret", "credentialsJSON:2fdd0a5a-1271-4ed6-9e53-83a888c1d63d", display = ParameterDisplay.HIDDEN)
        text("privacyManifestsPluginDeployVersion", "0.0.1", description = """Specify deployment version in pattern \d.\d.\d""", display = ParameterDisplay.PROMPT, allowEmpty = false)
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Prepare gnupg"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            scriptContent = """
                cd libraries
                export HOME=${'$'}(pwd)
                export GPG_TTY=${'$'}(tty)
                
                rm -rf .gnupg
                
                cat >keyfile <<EOT
                %sign.key.private.new%
                EOT
                gpg --allow-secret-key-import --batch --import keyfile
                rm -v keyfile
                
                cat >keyfile <<EOT
                %sign.key.main.public%
                EOT
                gpg --batch --import keyfile
                rm -v keyfile
            """.trimIndent()
        }
        gradle {
            name = "Publish Gradle plugin"
            tasks = ":kotlin-privacy-manifests-plugin:publishPlugins"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -PprivacyManifestsPluginDeployVersion=%privacyManifestsPluginDeployVersion% -Pkotlin.apple.applePrivacyManifestsPlugin=true --no-scan --no-build-cache -PsigningRequired=true -Psigning.gnupg.executable=gpg -Psigning.gnupg.keyName=%sign.key.id.new% -Psigning.gnupg.homeDir=%teamcity.build.checkoutDir%/kotlin/libraries/.gnupg -Psigning.gnupg.passphrase=%sign.key.passphrase.new%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Cleanup"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            scriptContent = """
                cd libraries
                rm -rf .gnupg
            """.trimIndent()
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
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(PrivacyManifestsPluginPublication) {
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "-deployment")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
