package Service.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object PublishReleaseStatusCheckBot : BuildType({
    name = "🐧 Publish release status to Slack"

    params {
        text("kotlinVersion", "master", label = "Kotlin version", description = "Kotlin version used for the report header", display = ParameterDisplay.PROMPT, allowEmpty = false)
        password("slackToken", "credentialsJSON:64b373f0-4208-4d46-a2a3-b2f82c5b3684", display = ParameterDisplay.HIDDEN)
        text("kotlinLv", "2.1", label = "Kotlin language version", description = "Kotlin language version", display = ParameterDisplay.PROMPT, allowEmpty = false)
        password("youtrackToken", "credentialsJSON:50949d2b-85f3-4ec1-b9ad-62941388097a", display = ParameterDisplay.HIDDEN)
        text("kotlinBranch", "master", label = "Kotlin branch", description = "Kotlin branch", display = ParameterDisplay.PROMPT, allowEmpty = false)
        select("profile", "ReleaseReportProd", label = "Report profile", display = ParameterDisplay.PROMPT,
                options = listOf("ReleaseReportProd", "ReleaseReportDev"))
    }

    vcs {
        root(Service.vcsRoots.PublishReleaseStatusCheckVCS, "+:. => publish-release-status-check")
    }

    steps {
        gradle {
            name = "Generate status for kotlin 2.1.0 release"
            tasks = """
                clean test --tests %profile% -Drelease.bot.kotlin.version=%kotlinVersion%
                                -Drelease.bot.kotlin.branch=%kotlinBranch% -Drelease.bot.kotlin.lv=%kotlinLv%
                                -Dteamcity.internal.token=%teamcity.serviceUser.token%
                                -Dteamcity.cloud.token=%teamcity.cloud.serviceUser.token% -Dyoutrack.token=%youtrackToken%
                                -Dslack.token=%slackToken%
            """.trimIndent()
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/publish-release-status-check"
            enableStacktrace = true
            jdkHome = "%env.JDK_17_0%"
        }
    }

    triggers {
        schedule {
            enabled = false
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Wednesday
                hour = 10
            }
            triggerBuild = always()
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
