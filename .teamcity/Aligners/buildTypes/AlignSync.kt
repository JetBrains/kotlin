package Aligners.buildTypes

import _Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object AlignSync : BuildType({
    name = "🐧 Align Sync"
    description = "No-op configuration to sync kotlin/master and intellij/kt-master"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(_Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master, "+:. => intellij-kt-master")

        checkoutMode = CheckoutMode.MANUAL
        branchFilter = "+:<default>"
    }

    steps {
        script {
            name = "Add hashes to status"
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                short_kotlin_hash=${'$'}(echo "${DslContext.settingsRoot.paramRefs.buildVcsNumber}" | head -c8)
                short_intellij_hash=${'$'}(echo "${IntellijMonorepoForKotlinVCS_kt_master.paramRefs.buildVcsNumber}" | head -c8)
                
                echo "##teamcity[buildStatus text='kt:${'$'}short_kotlin_hash ij:${'$'}short_intellij_hash {build.status.text}']"
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 300
            branchFilter = "+:<default>"
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }

    cleanup {
        keepRule {
            id = "Keep logs and stats for 90 days"
            keepAtLeast = days(90)
            dataToKeep = historyAndStatistics()
        }
    }
})
