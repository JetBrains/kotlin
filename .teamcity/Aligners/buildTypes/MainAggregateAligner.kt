package Aligners.buildTypes

import Aligners.vcsRoots.IntellijKtMasterVCS
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object MainAggregateAligner : BuildType({
    name = "🐧 Aligner for Aggregate"
    description = """
        Align Kotlin and IntelliJ repos for the cooperative build.
        [Aggregate](https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_BuildPlayground_Aquarius_Aggregate) is scheduled with the REST API for non-default branches.
        [More information about Aligner](https://youtrack.jetbrains.com/articles/KT-A-311/Aligner)
    """.trimIndent()

    params {
        param("teamcity.perfmon.feature.enabled", "false")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(Aligners.vcsRoots.IntellijKtMasterVCS, "+:. => intellij-kt-master")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        script {
            name = "Set align status"
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                is_default_branch="%teamcity.build.branch.is_default%"
                
                intellij_branch_current="${IntellijKtMasterVCS.paramRefs.buildVcsBranch}"
                intellij_branch_default="refs/heads/${IntellijKtMasterVCS.paramRefs["branch"]}"
                
                kotlin_branch_current="${DslContext.settingsRoot.paramRefs.buildVcsBranch}"
                kotlin_branch_default="refs/heads/${DslContext.settingsRoot.paramRefs["branch"]}"
                
                if [[ "${'$'}is_default_branch" == "true" ]]; then
                  echo "##teamcity[buildStatus text='No align for default branches {build.status.text}']"
                else
                  if [[ "${'$'}intellij_branch_current" == "${'$'}intellij_branch_default" ]]; then
                    full_commit_hash=${AlignDivergeCommitSearch.depParamRefs["kotlin_diverge_commit"]}
                    short_commit_hash=${'$'}{full_commit_hash:0:6}
                
                    echo "##teamcity[buildStatus text='${AlignPushLatest.depParamRefs["push_status"]} ${AlignDivergeCommitSearch.depParamRefs["kotlin_diverge_commit_date"]} kt-${'$'}short_commit_hash {build.status.text}']"
                  else
                    if [[ "${'$'}kotlin_branch_current" == "${'$'}kotlin_branch_default" ]]; then
                      echo "##teamcity[buildStatus text='IntelliJ branch with no Kotlin branch {build.status.text}']"
                    else
                      echo "##teamcity[buildStatus text='IntelliJ and Kotlin branches exist {build.status.text}']"
                    fi
                  fi
                fi
            """.trimIndent()
        }
        script {
            name = "Trigger desired build"

            conditions {
                equals("teamcity.build.branch.is_default", "false")
            }
            scriptContent = """
                #!/bin/bash
                                    set -e
                                    set -x
                                    
                                    curl -X POST %teamcity.serverUrl%/app/rest/buildQueue \
                                    -H "Authorization: Bearer %teamcity.serviceUser.token%" \
                                    -H "Content-Type: application/xml" \
                                    -H "Origin: %teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id%" \
                                    -d '
                                    <build branchName="%teamcity.build.branch%" comment="Started from %teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id%">
                                        <buildType id="Kotlin_BuildPlayground_Aquarius_Aggregate"/>
                                        <properties>
                                            <property name="teamcity.build.triggeredBy" value="%teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id%"/>
                                            <property name="teamcity.build.triggeredBy.username" value="%teamcity.build.triggeredBy.username% - Rest API teamcity.serviceUser.token"/>
                                        </properties>
                                    </build>'
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            enabled = false
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 120
            branchFilter = """
                +:<default>
                +:rr/*
                +:rrn/*
                +:push/*
            """.trimIndent()
        }
    }

    dependencies {
        snapshot(AlignDivergeCommitSearch) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(AlignPushLatest) {
            reuseBuilds = ReuseBuilds.NO
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
