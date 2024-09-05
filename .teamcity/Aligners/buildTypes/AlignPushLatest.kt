package Aligners.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object AlignPushLatest : BuildType({
    name = "🐧 Align push for IntelliJ Monorepo Latest"
    description = "https://jetbrains.team/p/ij/repositories/intellij/commits?query=head%3Arefs%2Fheads%2Fkt-master&tab=changes"

    params {
        param("correspondent_commit", "Correspondent commit to diverge commit in Kotlin for `IntelliJ (kt-master) with aligner references` in `kt-master` branch (will be set during execution)")
        param("align-tool-version", "0.1.16")
        param("push_status", "undefined_status")
        param("teamcity.perfmon.feature.enabled", "false")
    }

    vcs {
        root(_Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master, "+:. => intellij-kt-master")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        script {
            name = "Check default branch"

            conditions {
                equals("teamcity.build.branch.is_default", "true")
            }
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                echo "##teamcity[setParameter name='push_status' value='default']"
            """.trimIndent()
        }
        script {
            name = "Check custom branch"

            conditions {
                startsWith("push_status", "undefined_status")
            }
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                isBranchPresent=${'$'}(git ls-remote --heads ssh://git@git.jetbrains.team/intellij.git %teamcity.build.branch% | wc -l)
                if [[ "${'$'}isBranchPresent" == 1 ]]; then
                  echo "##teamcity[setParameter name='push_status' value='custom']"
                fi
                
                
                if [ -d "intellij-kt-master" ]
                then
                    echo "Checkout directory intellij-kt-master exists"
                else
                    echo "Checkout directory does not exist"
                    mkdir intellij-kt-master
                    cd intellij-kt-master
                    git init --bare
                    git remote add origin ssh://git@git.jetbrains.team/intellij.git
                    cd ..
                fi
                
                cd intellij-kt-master
                git push origin --delete refs/buildserver/kotlin-align/kt-master/%teamcity.build.branch%
            """.trimIndent()
        }
        script {
            name = "Find correspondent commit"

            conditions {
                startsWith("push_status", "undefined_status")
            }
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                intellij_correspondent_commit=${'$'}(./git-align/bin/git-align find teamcity \
                  --server=%teamcity.serverUrl% \
                  --token=%teamcity.serviceUser.token% \
                  --configuration-id=Kotlin_KotlinDev_AlignSync \
                  --vcs-id=Kotlin_KotlinDev_Kotlin \
                  --hash=${AlignDivergeCommitSearch.depParamRefs["kotlin_diverge_commit"]} \
                  --date="${AlignDivergeCommitSearch.depParamRefs["kotlin_diverge_commit_date"]}" \
                  --matching-property="build.vcs.number.Kotlin_KotlinDev_IntellijMonorepoForKotlinVCS_kt_master" \
                  --log=info \
                )
                
                echo "##teamcity[setParameter name='correspondent_commit' value='${'$'}intellij_correspondent_commit']"
            """.trimIndent()
        }
        script {
            name = "Push To Align Reference"

            conditions {
                startsWith("push_status", "undefined_status")
            }
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                
                if [ -d "intellij-kt-master" ]
                then
                    echo "Checkout directory intellij-kt-master exists"
                else
                    echo "Checkout directory does not exist"
                    mkdir intellij-kt-master
                    cd intellij-kt-master
                    git init --bare
                    git remote add origin ssh://git@git.jetbrains.team/intellij.git
                    cd ..
                fi
                
                cd intellij-kt-master
                git fetch --no-tags --shallow-since="90 days ago" origin kt-master
                git push origin %correspondent_commit%:refs/buildserver/kotlin-align/kt-master/%teamcity.build.branch% -f
                cd .. 
                
                full_commit_hash=%correspondent_commit%
                short_commit_hash=${'$'}{full_commit_hash:0:6}
                
                echo "##teamcity[setParameter name='push_status' value='${'$'}short_commit_hash']"
            """.trimIndent()
        }
        script {
            name = "Set push status"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                full_commit_hash=${AlignDivergeCommitSearch.depParamRefs["kotlin_diverge_commit"]}
                short_commit_hash=${'$'}{full_commit_hash:0:6}
                                
                echo "##teamcity[buildStatus text='%push_status% ${AlignDivergeCommitSearch.depParamRefs["kotlin_diverge_commit_date"]} kt-${'$'}short_commit_hash {build.status.text}']"            
                echo "##teamcity[setParameter name='push_status' value='kt-master-%push_status%']"
            """.trimIndent()
        }
    }

    features {
        sshAgent {
            teamcitySshKey = "jb.team -_ Applications -_ Kotlin Infrastructure Auto-Push"
        }
    }

    dependencies {
        snapshot(AlignDivergeCommitSearch) {
            reuseBuilds = ReuseBuilds.NO
        }
        artifacts(AbsoluteId("Kotlin_ServiceTasks_GitAlignUtils")) {
            buildRule = build("%align-tool-version%")
            artifactRules = "git-align-shadow-%align-tool-version%.zip!/git-align-shadow-%align-tool-version%/**=>git-align"
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
