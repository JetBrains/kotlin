package Service.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.qodana
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object QodanaGradleScan : BuildType({
    name = "🐧 Qodana Gradle dependencies Scan"

    params {
        param("env.JAVA_HOME", "%env.JDK_11_0%")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "write qodana.yaml"
            workingDir = "kotlin"
            scriptContent = """
                cat >qodana.yaml <<EOF
                version: "1.0"
                disableSanityInspections: true
                profile:
                  name: empty
                include:
                  - name: VulnerableLibrariesGlobal
                  - name: VulnerableLibrariesLocal
                EOF
            """.trimIndent()
        }
        qodana {
            name = "Run Qodana"
            workingDir = "kotlin"
            linter = customLinter {
                image = "kotlin.registry.jetbrains.space/p/kotlin/containers/kotlin-qodana:v3"
            }
            additionalDockerArguments = """
                -e QODANA_REMOTE_URL="%vcsroot.url%"
                -e QODANA_BRANCH="%vcsroot.branch%"
                -e QODANA_REVISION="%build.vcs.number%"
            """.trimIndent()
            cloudToken = "credentialsJSON:796c5f7d-235b-4c30-ba76-9c6656a84a83"
            param("collect-anonymous-statistics", "")
            param("report-as-test", "")
        }
    }

    triggers {
        schedule {
            enabled = false
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Monday
                hour = 0
            }
            branchFilter = "+:<default>"
            triggerBuild = always()
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "31000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "33000")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
