package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object TriggerUserProjectsTcc : BuildType({
    name = "🐧 Trigger User Projects in TeamCity Cloud"

    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        script {
            name = "Trigger desired build"
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                set -x
                
                curl -X POST https://kotlinlang.teamcity.com/app/rest/buildQueue \
                -H "Authorization: Bearer %teamcity.cloud.serviceUser.token%" \
                -H "Content-Type: application/json" \
                -H "Accept: application/json" \
                -H "Origin: %teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id%" \
                -d '
                {
                  "branchName": "%teamcity.build.branch%",
                  "buildType": {
                    "id": "Kotlin_KotlinCloud_BuildPlayground_Aquarius_UserProjectsAggregateK2"
                  },
                  "revisions": {
                    "revision": [
                      {
                        "version": "${DslContext.settingsRoot.paramRefs.buildVcsNumber}",
                        "vcsBranchName": "${DslContext.settingsRoot.paramRefs.buildVcsBranch}",
                        "vcs-root-instance": {
                          "vcs-root-id": "Kotlin_KotlinCloud_BuildPlayground_Aquarius_Kotlin"
                        }
                      }
                    ],
                    "failOnMissingRevisions": false
                  },
                  "comment": {
                    "text": "Triggered from %teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id%"
                  },
                  "properties": {
                    "property": [
                      {
                        "name": "reverse.dep.*.kotlin.version",
                        "value": "${BuildNumber.depParamRefs["deployVersion"]}"
                      }
                    ]
                  }
                }
                ' > response.json
                cat response.json
                webUrl=${'$'}(cat response.json | jq --raw-output '.["webUrl"]')
                echo ${'$'}webUrl
                echo "##teamcity[buildStatus text='${'$'}webUrl']"
            """.trimIndent()
        }
    }

    features {
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        snapshot(Deploy.buildTypes.DeployUserProjectsArtifacts) {
            onDependencyFailure = FailureAction.FAIL_TO_START
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
