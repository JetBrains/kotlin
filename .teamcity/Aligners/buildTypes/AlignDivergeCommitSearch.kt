package Aligners.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object AlignDivergeCommitSearch : BuildType({
    name = "🐧 Align: Kotlin Diverge Commit Finder"
    description = "Find common commit for the triggered branch and master in Kotlin repository"

    params {
        param("kotlin_diverge_commit", "undefined_commit")
        param("kotlin_diverge_commit_date", "undefined_date")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
    }

    steps {
        script {
            name = "Find diverge commit in Kotlin"

            conditions {
                equals("teamcity.build.branch.is_default", "false")
            }
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                cd kotlin
                git fetch origin master
                
                diverge_commit=${'$'}(git merge-base HEAD origin/master)
                diverge_commit_date=${'$'}(git show --format=%cd --no-patch --date=format:'%Y/%m/%d' ${'$'}diverge_commit)
                
                echo "##teamcity[setParameter name='kotlin_diverge_commit' value='${'$'}diverge_commit']"
                echo "##teamcity[setParameter name='kotlin_diverge_commit_date' value='${'$'}diverge_commit_date']"
                
                short_commit_hash=${'$'}{diverge_commit:0:10}
                echo "##teamcity[buildStatus text='${'$'}short_commit_hash ${'$'}diverge_commit_date {build.status.text}']"
                
                cd ..
            """.trimIndent()
        }
    }

    features {
        sshAgent {
            teamcitySshKey = "Kotlin_project_at_buildserver.labs.intellij.net_https___github.com_KotlinBuild"
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
