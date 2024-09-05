package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object RelayNightlyDevToSonatype : BuildType({
    name = "🐧 Relay to sonatype from kotlin.space (kotlin dev) (NIGHTLY)"

    enablePersonalBuilds = false
    artifactRules = "space=>space"
    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs["deployVersion"]}:%build.counter%"
    maxRunningBuilds = 1

    params {
        password("space-password", "credentialsJSON:6a448008-2992-4f63-9a64-2e5013887a2e", display = ParameterDisplay.PROMPT)
        text("space-username", "%space.kotlin.packages.user%", display = ParameterDisplay.PROMPT, allowEmpty = false)
        param("sonatype-clean-description-prefix", "%system.teamcity.buildType.id%:")
        text("sonatype-repository-id-create", "create-id", display = ParameterDisplay.HIDDEN, allowEmpty = false)
        text("space-repo-name", "dev", display = ParameterDisplay.PROMPT, allowEmpty = false)
        param("publishing-util-version", "0.1.95")
        text("filtered-packages", """
            [\
              org.jetbrains.kotlin.experimental.compose:compiler,\
              org.jetbrains.kotlin.experimental.compose:compiler-daemon,\
              org.jetbrains.kotlin.experimental.compose:compiler-hosted,\
              ]
        """.trimIndent(), display = ParameterDisplay.PROMPT, allowEmpty = false)
        text("sonatype-repository-id", "%sonatype-repository-id-create%", display = ParameterDisplay.PROMPT, allowEmpty = false)
        password("sonatype-password", "credentialsJSON:81999f5f-6084-4108-ab95-4d4ad6bd57c4")
        param("sonatype-repository-key", "%sonatype-clean-description-prefix%%build.number%")
        text("version-to-relay", "${BuildNumber.depParamRefs["deployVersion"]}", display = ParameterDisplay.PROMPT, allowEmpty = false)
        text("sonatype-profile-id", "169b36e205a64e", display = ParameterDisplay.PROMPT, allowEmpty = true)
        param("sonatype-repository-description", "Publishing %version-to-relay% from %space-url% %space-project-key% %space-repo-name%")
        param("teamcity-parameter", "sonatype-repository-id")
        param("sonatype-username", "%sonatype.user%")
        text("space-project-key", "KOTLIN", display = ParameterDisplay.PROMPT, allowEmpty = true)
        text("space-url", "https://kotlin.jetbrains.space", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    steps {
        script {
            name = "Create Repository"
            scriptContent = """
                #!/bin/bash
                
                repositoryId="%sonatype-repository-id%"
                
                if [[ "${'$'}repositoryId" == "%sonatype-repository-id-create%" ]]; then
                  ./publishing-utils/bin/publishing-utils \
                    sonatype-repository-create \
                      --sonatype-username="%sonatype-username%" \
                      --sonatype-password="%sonatype-password%" \
                      --sonatype-profile-id="%sonatype-profile-id%" \
                      --sonatype-repository-description="%sonatype-repository-description%" \
                      --sonatype-repository-key="%sonatype-repository-key%" \
                      --teamcity-parameter="%teamcity-parameter%"
                else
                  echo "Repository id is preset: ${'$'}repositoryId"
                fi
            """.trimIndent()
        }
        script {
            name = "Download from Space and publish to Sonatype"
            scriptContent = """
                #!/bin/bash
                
                ./publishing-utils/bin/publishing-utils \
                  space-to-sonatype \
                  --sonatype-username="%sonatype-username%" \
                  --sonatype-password="%sonatype-password%" \
                  --sonatype-repository-id="%sonatype-repository-id%" \
                  --space-url="%space-url%" \
                  --space-username="%space-username%" \
                  --space-password="%space-password%" \
                  --from-project-key="%space-project-key%" \
                  --from-repo-name="%space-repo-name%" \
                  --version="%version-to-relay%" \
                  --hashes=[md5,sha1] \
                  --filtered-packages="%filtered-packages%"
            """.trimIndent()
        }
        script {
            name = "Remove previous publications"
            executionMode = BuildStep.ExecutionMode.RUN_ON_SUCCESS
            scriptContent = """
                #!/bin/bash
                
                ./publishing-utils/bin/publishing-utils \
                  sonatype-repository-clean \
                  --sonatype-username="%sonatype-username%" \
                  --sonatype-password="%sonatype-password%" \
                  --sonatype-profile-id="%sonatype-profile-id%" \
                  --sonatype-clean-description-prefix="%sonatype-clean-description-prefix%" \
                  --sonatype-preserve-repository-ids=[%sonatype-repository-id%]
            """.trimIndent()
        }
        script {
            name = "Close and Verify"
            scriptContent = """
                #!/bin/bash
                
                ./publishing-utils/bin/publishing-utils \
                  sonatype-repository-close \
                  --sonatype-username="%sonatype-username%" \
                  --sonatype-password="%sonatype-password%" \
                  --sonatype-profile-id="%sonatype-profile-id%" \
                  --sonatype-repository-id="%sonatype-repository-id%" \
                  --sonatype-repository-verify-pause-seconds=180
            """.trimIndent()
        }
    }

    features {
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "#kotlin-bots"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            buildFailed = true
        }
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        snapshot(DeployMavenArtifacts_Nightly_kotlin_space_packages) {
            reuseBuilds = ReuseBuilds.ANY
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        artifacts(AbsoluteId("Kotlin_ServiceTasks_BintrayUtils_Build")) {
            buildRule = build("%publishing-util-version%")
            artifactRules = "publishing-utils-%publishing-util-version%.zip!/publishing-utils-%publishing-util-version%/**=>publishing-utils"
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-deployment")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
