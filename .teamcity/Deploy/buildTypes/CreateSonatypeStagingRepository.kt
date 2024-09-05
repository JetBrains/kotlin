package Deploy.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object CreateSonatypeStagingRepository : BuildType({
    name = "🐧 Create Sonatype Staging Repository"

    type = BuildTypeSettings.Type.DEPLOYMENT

    params {
        param("system.kotlin.sonatype.profile.id", "169b36e205a64e")
        param("system.kotlin.sonatype.id", "")
        param("publishing-util-version", "0.1.95")
    }

    steps {
        script {
            name = "Create sonatype staging repository"
            scriptContent = """
                #!/bin/bash
                ./publishing-utils/bin/publishing-utils \
                sonatype-repository-create \
                --sonatype-username="%sonatype.user%" \
                --sonatype-password="%sonatype.password%" \
                --teamcity-parameter="%system.kotlin.sonatype.id%" \
                --sonatype-profile-id="%system.kotlin.sonatype.profile.id%" \
                --sonatype-repository-description="Kotlin staging repository"
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
