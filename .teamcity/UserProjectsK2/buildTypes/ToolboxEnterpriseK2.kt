package UserProjectsK2.buildTypes

import jetbrains.buildServer.configs.kotlin.*

object ToolboxEnterpriseK2 : BuildType({
    name = "🐧 [Project] Toolbox Enterprise with K2"

    buildNumberPattern = "%dep.TBE_Builds_TestKotlinDev.build.number%"

    dependencies {
        snapshot(AbsoluteId("TBE_Builds_TestKotlinDev")) {
        }
    }

    requirements {
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
