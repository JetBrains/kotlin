package KotlinxTrainProjectK2.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object KotlinxTrainSerializationK2 : BuildType({
    name = "🐧 Kotlinx.train serialization K2 build"

    artifactRules = """
        %train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx
        **/*.hprof=>internal/hprof.zip
    """.trimIndent()
    buildNumberPattern = "%DeployVersion% (%build.counter%)"

    params {
        param("branch.serialization", "kotlin-community/dev")
        param("train.maven.local.repository.path", "%teamcity.build.checkoutDir%/%train.maven.local.repository.dir%")
        param("DeployVersion", "2.2.3-train-%kotlin_snapshot_version%")
        param("kotlin_snapshot_version", "")
        param("train.maven.local.repository.dir", ".localrepo")
    }

    vcs {
        root(_Self.vcsRoots.SerializationVCS, "+:. => project.serialization")
    }

    steps {
        gradle {
            name = "Build serialization project"
            tasks = "build"
            buildFile = "build.gradle.kts"
            workingDir = "project.serialization"
            gradleParams = """
                -x kotlinStoreYarnLock
                    -x knitCheck 
                    -x apiCheck  
                    -x kotlinStoreYarnLock
                    --stacktrace 
                    --continue 
                    -Pnative.deploy=all 
                    -Pbuild_snapshot_up=true 
                    -Dmaven.repo.local=%train.maven.local.repository.path%  
                    -Pbuild_snapshot_train=true 
                    -Pkotlin.version=%kotlin_snapshot_version% 
                    -Pkotlin_snapshot_version=%kotlin_snapshot_version% 
                    -Pkotlin_language_version=2.0 
                    -PDeployVersion=%DeployVersion% 
                    -Dmaven.repo.local=%train.maven.local.repository.path% 
                    -PDeployVersion=%DeployVersion%
            """.trimIndent()
            jdkHome = "%env.JDK_11%"
        }
        gradle {
            name = "Publish serialization to maven local"
            tasks = "publishToMavenLocal"
            buildFile = "build.gradle.kts"
            workingDir = "project.serialization"
            gradleParams = """
                -x kotlinStoreYarnLock
                    -x knitCheck 
                    -x apiCheck  
                    -x kotlinStoreYarnLock
                    --stacktrace 
                    --continue 
                    -Pnative.deploy=all 
                    -Pbuild_snapshot_up=true 
                    -Dmaven.repo.local=%train.maven.local.repository.path%  
                    -Pbuild_snapshot_train=true 
                    -Pkotlin.version=%kotlin_snapshot_version% 
                    -Pkotlin_snapshot_version=%kotlin_snapshot_version% 
                    -Pkotlin_language_version=2.0 
                    -PDeployVersion=%DeployVersion% 
                    -Dmaven.repo.local=%train.maven.local.repository.path% 
                    -PDeployVersion=%DeployVersion%
            """.trimIndent()
            jdkHome = "%env.JDK_11%"
        }
    }

    failureConditions {
        executionTimeoutMin = 45
        supportTestRetry = true
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
