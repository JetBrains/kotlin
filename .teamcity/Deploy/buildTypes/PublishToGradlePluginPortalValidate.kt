package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object PublishToGradlePluginPortalValidate : BuildType({
    name = "🐧 Publish to Gradle Plugin Portal (Validate only)"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${DeployKotlinMavenArtifacts.depParamRefs.buildNumber}"

    params {
        password("system.gradle.publish.key", "credentialsJSON:08dfddc9-529c-48dd-95d1-68c376e53c9e", display = ParameterDisplay.HIDDEN)
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("DeployVersion", "${BuildNumber.depParamRefs["deployVersion"]}")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        param("system.deployVersion", "%DeployVersion%")
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        password("system.gradle.publish.secret", "credentialsJSON:2fdd0a5a-1271-4ed6-9e53-83a888c1d63d", display = ParameterDisplay.HIDDEN)
        param("system.versions.kotlin-native", "${DeployKotlinMavenArtifacts.depParamRefs["versions.kotlin-native"]}")
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
            name = "Gradle plugins to Gradle Plugin Portal (Validate only)"
            tasks = "publishPlugins --validate-only"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel --no-build-cache --no-scan --info --stacktrace -PsigningRequired=true -Psigning.gnupg.executable=gpg -Psigning.gnupg.keyName=%sign.key.id.new% -Psigning.gnupg.homeDir=%teamcity.build.checkoutDir%/kotlin/libraries/.gnupg -Psigning.gnupg.passphrase=%sign.key.passphrase.new%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Cleanup"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            workingDir = "libraries"
            scriptContent = """
                cd .
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
        snapshot(DeployKotlinMavenArtifacts) {
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
