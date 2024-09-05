package Service.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object ArtifactSignatures : BuildType({
    name = "🐧 Generate signatures for Kotlin/Native artifacts (checksums and gpg signatures)"

    artifactRules = "%teamcity.build.checkoutDir%/artifact-signatures/artifacts/** => signatures.zip"

    params {
        param("env.ARTIFACT_SIGNATURE_ARTIFACTS_TARGET_DIR", "%teamcity.build.checkoutDir%/artifact-signatures/download")
        param("env.ARTIFACT_SIGNATURE_GPG_PASSPHRASE", "%sign.key.passphrase.new%")
        param("env.GNUPGHOME", "%teamcity.build.checkoutDir%/artifact-signatures/.gnupg")
        param("gradleParameters", "%globalGradleParameters%")
    }

    vcs {
        root(Service.vcsRoots.ArtifactSignaturesVCS, "+:. => artifact-signatures")
    }

    steps {
        script {
            name = "Prepare gnupg"
            workingDir = "%teamcity.build.checkoutDir%/artifact-signatures"
            scriptContent = """
                cd .
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
            name = "Run artifact-signatures"
            tasks = ":run"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/artifact-signatures"
            gradleParams = """%gradleParameters% --parallel --args="generate""""
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Remove binary artifacts from %teamcity.build.checkoutDir%/artifact-signatures/download"
            scriptContent = """
                mkdir %teamcity.build.checkoutDir%/artifact-signatures/artifacts 
                
                mv %teamcity.build.checkoutDir%/artifact-signatures/download/*.asc %teamcity.build.checkoutDir%/artifact-signatures/artifacts
                mv %teamcity.build.checkoutDir%/artifact-signatures/download/*.sha256 %teamcity.build.checkoutDir%/artifact-signatures/artifacts
                
                rm -rf %teamcity.build.checkoutDir%/artifact-signatures/download
            """.trimIndent()
        }
        script {
            name = "Cleanup"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            workingDir = "%teamcity.build.checkoutDir%/artifact-signatures"
            scriptContent = """
                cd .
                rm -rf .gnupg
            """.trimIndent()
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
