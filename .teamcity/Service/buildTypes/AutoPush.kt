package Service.buildTypes

import _Self.buildTypes.Aggregate
import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.AutoMerge
import jetbrains.buildServer.configs.kotlin.buildFeatures.merge
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

object AutoPush : BuildType({
    name = "🐧 Auto Push (push/*)"

    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("teamcity.jetbrains.git.autoMergeOptions", "git.merge.rebase=true")
        param("teamcity.internal.autoMerge.reportResultToBuildText", "true")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(_Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master, "+:. => intellij-kt-master")

        cleanCheckout = true
    }

    steps {
        script {
            name = "Set up Git"
            scriptContent = """
                "%env.TEAMCITY_GIT_PATH%" -C "%teamcity.build.checkoutDir%/kotlin" config user.email teamcity-demo-noreply@jetbrains.com
                "%env.TEAMCITY_GIT_PATH%" -C "%teamcity.build.checkoutDir%/kotlin" config user.name TeamCity
            """.trimIndent()
        }
        script {
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                cd %teamcity.build.checkoutDir%/kotlin
                git fetch --progress --no-tags --recurse-submodules=no origin ${DslContext.settingsRoot.paramRefs["branch"]}
                git rebase origin/${DslContext.settingsRoot.paramRefs["branch"]} || echo "##teamcity[buildProblem description='merge conflict in kotlin repo']"
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${Aggregate.id}"
            successfulOnly = true
            branchFilter = """
                +:push/*
                +:<default>
            """.trimIndent()
        }
    }

    features {
        merge {
            branchFilter = """
                +:*
                -:<default>
            """.trimIndent()
            mergePolicy = AutoMerge.MergePolicy.FAST_FORWARD
            runPolicy = AutoMerge.RunPolicy.BEFORE_BUILD_FINISH
        }
        sshAgent {
            teamcitySshKey = "Kotlin_project_at_buildserver.labs.intellij.net_https___github.com_KotlinBuild"
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.Aggregate) {
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
})
