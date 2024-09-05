package Service.buildTypes

import _Self.buildTypes.Aggregate
import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

object BranchAncestorAnalyzer : BuildType({
    name = "🐧 Branch Ancestor Analyzer"

    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("ancestorDate", "undefined")
        param("ancestorHash", "undefined")
    }

    vcs {
        root(Service.vcsRoots.BranchAncestorAnalyzer_1, "+:. => build-failure-analyzer")
        root(DslContext.settingsRoot, "+:. => kotlin")
    }

    steps {
        script {
            name = "Compute ancestor hash and date"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            scriptContent = """
                #!/bin/bash
                set -e # Exit if a command returns a non-zero status
                set -x # Print command trace
                
                git fetch origin master
                
                ancestor_hash=${'$'}(git merge-base HEAD origin/master)
                ancestor_date=${'$'}(git show --no-patch --format=%cs ${'$'}ancestor_hash)
                echo "##teamcity[setParameter name='ancestorHash' value='${'$'}ancestor_hash']"
                echo "##teamcity[setParameter name='ancestorDate' value='${'$'}ancestor_date']"
            """.trimIndent()
        }
        gradle {
            name = "Build analyzer"
            tasks = "branch-ancestor-analyzer:installDist"
            workingDir = "%teamcity.build.checkoutDir%/build-failure-analyzer"
            gradleParams = "%globalGradleParameters% -Dorg.gradle.daemon=false"
        }
        script {
            name = "Run analyzer"
            workingDir = "%teamcity.build.checkoutDir%/build-failure-analyzer/branch-ancestor-analyzer/build/install/branch-ancestor-analyzer/bin"
            scriptContent = """
                #!/bin/bash
                set -x
                
                ./branch-ancestor-analyzer --build-id=${Aggregate.depParamRefs["teamcity.build.id"]} --ancestor-hash=%ancestorHash% --ancestor-date=%ancestorDate% --token=%teamcity.serviceUser.token% --build-type=Kotlin_KotlinDev_Aggregate
            """.trimIndent()
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${Aggregate.id}"
            successfulOnly = false
            branchFilter = """
                +:*
                -:<default>
            """.trimIndent()
        }
    }

    features {
        sshAgent {
            teamcitySshKey = "Kotlin_project_at_buildserver.labs.intellij.net_https___github.com_KotlinBuild"
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.Aggregate) {
            reuseBuilds = ReuseBuilds.ANY
            onDependencyFailure = FailureAction.IGNORE
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
