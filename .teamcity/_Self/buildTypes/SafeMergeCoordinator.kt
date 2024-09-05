package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object SafeMergeCoordinator : BuildType({
    name = "🐧 Safe-Merge Coordinator"
    description = "Run Kotlin_BuildPlayground_Aquarius_SafeMergeAggregate tests, using both Kotlin and IntelliJ repos. If there is a feature branch of the same name in the IntelliJ repo and a Merge-Request into kt-master is opened, changes from that branch are also passed to the tests."

    params {
        password("internalSpaceToken", "credentialsJSON:5d38a3a8-13d2-414c-86c1-5646d0d1d329", readOnly = true)
        password("publicSpaceToken", "credentialsJSON:756f7f08-ef1f-4f11-9024-ffbd9b889de4", readOnly = true)
        param("teamcity.ui.runButton.caption", "")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(_Self.vcsRoots.IntellijMonorepoForKotlinVCS_kt_master, "+:. => intellij-kt-master")

        checkoutMode = CheckoutMode.ON_AGENT
    }

    steps {
        script {
            name = "Run Coordinator"

            conditions {
                equals("teamcity.build.branch.is_default", "false")
            }
            scriptContent = """
                #!/bin/bash
                set -e
                set -x
                
                cd safe-merge-coordinator/
                
                if [ "%env.SPACE_MERGE_ROBOT_MODE%" = "Dry run" ]; then
                    DRY_RUN=true
                else
                    DRY_RUN=false
                fi
                
                cat > primary-repos.json << EOF
                [{"spaceInstanceUrl":"https://kotlin.jetbrains.space","spaceAccessTokenEnvVar":"PUBLIC_SPACE_TOKEN","projectKey":"KOTLIN","repoName":"kotlin"},{"spaceInstanceUrl":"https://jetbrains.team","spaceAccessTokenEnvVar":"INTERNAL_SPACE_TOKEN","projectKey":"KT","repoName":"kotlin"}]
                EOF
                cat > secondary-repos.json << EOF
                [{"spaceInstanceUrl":"https://jetbrains.team","spaceAccessTokenEnvVar":"INTERNAL_SPACE_TOKEN","projectKey":"IJ","repoName":"ultimate"},{"spaceInstanceUrl":"https://jetbrains.team","spaceAccessTokenEnvVar":"INTERNAL_SPACE_TOKEN","projectKey":"KT","repoName":"intellij"}]
                EOF
                
                JAVA_HOME=%env.JDK_21_0% SAFE_MERGE_COORDINATOR_DRY_RUN=${'$'}DRY_RUN PUBLIC_SPACE_TOKEN=%publicSpaceToken% INTERNAL_SPACE_TOKEN=%internalSpaceToken% ./safe-merge-coordinator-shadow-%dep.Kotlin_ServiceTasks_SafeMergeCoordinator_Build.version%/bin/safe-merge-coordinator --primary-repos=primary-repos.json --secondary-repos=secondary-repos.json --primary-mr-link=%env.SPACE_MERGE_REQUEST_URL% --secondary-repo-master=kt-master --primary-repo-working-dir=%teamcity.build.checkoutDir%/kotlin --secondary-repo-working-dir=%teamcity.build.checkoutDir%/intellij-kt-master --primary-vcs-id=Kotlin_BuildPlayground_Aquarius_Kotlin --secondary-vcs-id=Kotlin_BuildPlayground_Aquarius_IntellijMonorepoForKotlinVCS_kt_master,ijplatform_master_IntellijRemoteForKotlinLatestCompiler_master --current-build-branch=%teamcity.build.branch% --current-build-revision=%system.build.vcs.number.Kotlin_BuildPlayground_Aquarius_Kotlin% --coordinator-teamcity-build-url=%teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id% --coordinator-teamcity-build-id=%teamcity.build.id% --teamcity-server-url=%teamcity.serverUrl% --teamcity-token=%teamcity.serviceUser.token% --build-to-trigger=Kotlin_BuildPlayground_Aquarius_SafeMergeAggregate --triggered-by-username=%teamcity.build.triggeredBy.username%
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            enabled = false
            branchFilter = "+:<default>"
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = space {
                authType = connection {
                    connectionId = "PROJECT_EXT_2845"
                }
                projectKey = "KT"
            }
        }
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = space {
                authType = connection {
                    connectionId = "PROJECT_EXT_3432"
                }
                projectKey = "KOTLIN"
            }
        }
        sshAgent {
            teamcitySshKey = "jb.team -_ Applications -_ Kotlin Infrastructure Auto-Push"
        }
        sshAgent {
            teamcitySshKey = "Kotlin_project_at_buildserver.labs.intellij.net_https___github.com_KotlinBuild"
        }
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        artifacts(AbsoluteId("Kotlin_ServiceTasks_SafeMergeCoordinator_Build")) {
            buildRule = lastPinned()
            artifactRules = "safe-merge-coordinator-shadow-*.zip!/safe-merge-coordinator-shadow-*/**=>safe-merge-coordinator"
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
