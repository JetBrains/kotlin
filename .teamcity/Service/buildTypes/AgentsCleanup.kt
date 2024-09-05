package Service.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.matrix
import jetbrains.buildServer.configs.kotlin.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object AgentsCleanup : BuildType({
    name = "🦄 Agents Cleanup"

    steps {
        script {
            scriptContent = """
                #!/bin/bash
                
                rm -rf ~/.gradle
                rm -rf ~/.konan
                rm -rf ~/.m2
                rm -rf %system.agent.persistent.cache%/konan
                rm -rf %teamcity.agent.home.dir%/system/.artifacts_cache/*
            """.trimIndent()
        }
    }

    triggers {
        schedule {
            enabled = false
            schedulingPolicy = weekly {
                dayOfWeek = ScheduleTrigger.DAY.Monday
                hour = 0
            }
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    features {
        matrix {
            param("agent", listOf(
                value("kotlin-macos-x64-de-unit-1555"),
                value("kotlin-macos-x64-de-unit-1556"),
                value("kotlin-macos-x64-de-unit-1557"),
                value("kotlin-macos-x64-de-unit-1558"),
                value("kotlin-macos-x64-de-unit-1559"),
                value("kotlin-macos-x64-de-unit-1560"),
                value("kotlin-macos-x64-de-unit-1561"),
                value("kotlin-macos-x64-de-unit-1562"),
                value("kotlin-macos-x64-de-unit-1563"),
                value("kotlin-macos-x64-de-unit-1564"),
                value("kotlin-macos-x64-de-unit-1565"),
                value("kotlin-macos-x64-de-unit-1567"),
                value("kotlin-macos-x64-de-unit-1568"),
                value("kotlin-macos-x64-de-unit-1569"),
                value("kotlin-macos-m1-munit685"),
                value("kotlin-macos-m1-munit686"),
                value("kotlin-macos-m1-munit687"),
                value("kotlin-macos-m1-munit688"),
                value("kotlin-macos-m1-munit700"),
                value("kotlin-macos-m1-munit701"),
                value("kotlin-macos-m1-munit702"),
                value("kotlin-macos-m1-munit703"),
                value("kotlin-macos-m1-de-unit-1259"),
                value("kotlin-macos-m1-de-unit-1260"),
                value("kotlin-macos-m1-de-unit-1261"),
                value("kotlin-macos-m1-de-unit-1262"),
                value("kotlin-macos-m1-de-unit-1263"),
                value("kotlin-macos-m1-de-unit-1264"),
                value("kotlin-macos-m1-de-unit-1265"),
                value("kotlin-macos-m1-de-unit-1282"),
                value("kotlin-macos-m1-de-unit-1283"),
                value("kotlin-macos-m1-de-unit-1284"),
                value("kotlin-macos-m1-de-unit-1285"),
                value("kotlin-macos-m1-de-unit-1286"),
                value("kotlin-macos-m1-de-unit-1287"),
                value("kotlin-macos-m1-de-unit-1288"),
                value("kotlin-macos-m1-de-unit-1289"),
                value("kotlin-macos-m1-de-unit-1290")
            ))
        }
    }

    requirements {
        equals("teamcity.agent.name", "%agent%")
    }
})
