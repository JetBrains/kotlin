package UserProjectsK2.buildTypes

import _Self.buildTypes.CompilerDistAndMavenArtifactsForIde
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object IntellijK2 : BuildType({
    name = "🐧 [Project] IntelliJ monorepo with K2"

    buildNumberPattern = "${CompilerDistAndMavenArtifactsForIde.depParamRefs.buildNumber}  (%build.counter%)"

    params {
        param("user.project.api.version", "2.0")
        param("user.project.lv.version", "2.0")
    }

    vcs {
        root(UserProjectsK2.vcsRoots.IntellijK2VCS, "+:. => user-project")

        cleanCheckout = true
    }

    steps {
        script {
            name = "configure intellij monorepo as k2 user project"
            workingDir = "user-project"
            scriptContent = "./configureK2UserProject.sh ${CompilerDistAndMavenArtifactsForIde.depParamRefs.buildNumber} %user.project.lv.version% %user.project.api.version%"
        }
        script {
            name = "compile intellij monorepo"
            workingDir = "user-project"
            scriptContent = "./community/platform/jps-bootstrap/jps-bootstrap.sh -Djps.auth.spaceUsername=%space.packages.user% -Djps.auth.spacePassword=%space.packages.secret% %teamcity.build.checkoutDir%/user-project intellij.idea.ultimate.build LocalCompileBuildTarget"
        }
    }

    failureConditions {
        executionTimeoutMin = 200
    }

    features {
        notifications {
            enabled = false
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_486"
                sendTo = "kotlin-k2-user-projects-notifications"
                messageFormat = verboseMessageFormat {
                    addStatusText = true
                }
            }
            branchFilter = "+:<default>"
            buildFailed = true
            firstFailureAfterSuccess = true
        }
        freeDiskSpace {
            requiredSpace = "40gb"
            failBuild = true
        }
    }

    dependencies {
        dependency(_Self.buildTypes.CompilerDistAndMavenArtifactsForIde) {
            snapshot {
            }

            artifacts {
                artifactRules = "maven.zip!** => %teamcity.agent.jvm.user.home%/.m2/repository"
            }
        }
    }

    requirements {
        contains("system.cloud.profile_id", "aquarius-up-aws")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
