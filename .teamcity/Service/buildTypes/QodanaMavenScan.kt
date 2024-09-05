package Service.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.qodana
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object QodanaMavenScan : BuildType({
    name = "🐧 Qodana Maven dependencies Scan"

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
                cat >libraries/qodana.yaml <<EOF
                version: "1.0"
                disableSanityInspections: true
                profile:
                  name: empty
                bootstrap: sh ./prepare-maven.sh
                include:
                  - name: VulnerableLibrariesGlobal
                  - name: VulnerableLibrariesLocal
                EOF
            """.trimIndent()
        }
        script {
            name = "write bootstrap"
            workingDir = "kotlin"
            scriptContent = """
                cat >libraries/prepare-maven.sh <<EOF
                #!/bin/bash
                
                /data/project/gradlew install --project-dir=/data/project -Pkotlin.build.isObsoleteJdkOverrideEnabled=true -Pkotlin.build.cache.url=https://ge.jetbrains.com/cache/
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
            additionalQodanaArguments = "--project-dir /data/project/libraries"
            cloudToken = "credentialsJSON:be8c56fe-427e-4c0b-9d23-4bdde3d51cac"
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
