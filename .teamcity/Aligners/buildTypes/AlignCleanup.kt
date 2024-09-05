package Aligners.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object AlignCleanup : BuildType({
    name = "🐧 Align cleanup"
    description = "Remove old refs created by git-align"

    params {
        param("env.REF_PATTERN", "refs/buildserver/kotlin-align/kt-master")
    }

    vcs {
        root(_Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master, "+:. => intellij-kt-master")

        checkoutMode = CheckoutMode.ON_AGENT
        branchFilter = "+:<default>"
    }

    steps {
        script {
            workingDir = "%teamcity.build.checkoutDir%/intellij-kt-master"
            scriptContent = """
                #!/usr/bin/env bash
                
                set -u
                set -o pipefail
                
                _DRY_RUN=""
                # _DRY_RUN="--dry-run"
                
                if [ -z "${'$'}REF_PATTERN" ]; then
                  echo "Specify REF_PATTERN for align cleanup"
                  exit 1
                fi
                
                # change `-1 month` to desired distance in the past
                prev_date=${'$'}(date --date="${'$'}(date +%%Y-%%m-1) -1 month" --iso-8601 --utc)
                echo "Looking for refs not updated since: ${'$'}prev_date"
                
                readarray -t lines < <(git ls-remote | grep "${'$'}REF_PATTERN")
                for cur in "${'$'}{lines[@]}"; do
                  commit_hash=${'$'}(echo "${'$'}cur" | cut -f1)
                  ref_name=${'$'}(echo "${'$'}cur" | cut -f2)
                  log="${'$'}(git log -1 --since="${'$'}prev_date" --pretty=format:"%%cD%%x09%%h%%x09%%an%%x09%%s" "${'$'}commit_hash" 2>&1)"
                  if [[ -z "${'$'}log" || "${'$'}log" == "fatal: bad object"* ]]; then
                    echo "deleting: ${'$'}ref_name"
                    # shellcheck disable=SC2086
                    # double quoting of `${'$'}_DRY_RUN` breaks git command
                    git push ${'$'}_DRY_RUN origin --delete "${'$'}ref_name" 2>&1
                  fi
                done
            """.trimIndent()
        }
    }

    triggers {
        schedule {
            enabled = false
            schedulingPolicy = cron {
                hours = "6"
                dayOfWeek = "1-5"
            }
            branchFilter = "+:<default>"
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    features {
        sshAgent {
            teamcitySshKey = "jb.team -_ Applications -_ Kotlin Infrastructure Auto-Push"
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
