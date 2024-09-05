package KotlinxTrainProjectK2.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object KotlinxTrainAtomicfuK2 : BuildType({
    name = "🍎 kotlinx.atomicfu train K2"

    artifactRules = """
        %train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx
        **/*.hprof=>internal/hprof.zip
    """.trimIndent()
    buildNumberPattern = "%DeployVersion% (%build.counter%)"

    params {
        param("train.maven.local.repository.path", "%teamcity.build.checkoutDir%/%train.maven.local.repository.dir%")
        param("DeployVersion", "2.2.3-train-%kotlin_snapshot_version%")
        param("kotlin_snapshot_version", "")
        param("branch.atomicfu", "kotlin-community/dev")
        param("train.maven.local.repository.dir", ".localrepo")
    }

    vcs {
        root(KotlinxTrainProjectK2.vcsRoots.kotlinxatomicfutrain, "+:. => atomicfu")
    }

    steps {
        gradle {
            name = "Build atomicfu project"
            tasks = "build"
            buildFile = "build.gradle.kts"
            workingDir = "atomicfu"
            gradleParams = """
                -stacktrace -x check 
                --continue 
                 -x kotlinStoreYarnLock
                 -Pkotlin_repo_url=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev
                 -Pbuild_snapshot_train=true
                 -Pkotlin_language_version=2.0 
                 -Pkotlin_snapshot_version=%kotlin_snapshot_version%
                 -Pkotlin_version=%kotlin_snapshot_version%
                 -PDeployVersion=%DeployVersion%
                 -Dmaven.repo.local=%train.maven.local.repository.path%
            """.trimIndent()
            jdkHome = "%env.JDK_11%"
        }
        gradle {
            name = "Publish atomicfu To Maven Local"
            tasks = "publishToMavenLocal"
            buildFile = "build.gradle.kts"
            workingDir = "atomicfu"
            gradleParams = """
                -stacktrace -x check 
                --continue 
                 -x kotlinStoreYarnLock
                 -Pkotlin_repo_url=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev
                 -Pbuild_snapshot_train=true
                 -Pkotlin_language_version=2.0 
                 -Pkotlin_snapshot_version=%kotlin_snapshot_version%
                 -Pkotlin_version=%kotlin_snapshot_version%
                 -PDeployVersion=%DeployVersion%
                 -Dmaven.repo.local=%train.maven.local.repository.path%
            """.trimIndent()
            jdkHome = "%env.JDK_11%"
        }
    }

    failureConditions {
        executionTimeoutMin = 30
        supportTestRetry = true
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        startsWith("teamcity.agent.name", "kotlin-macos")
    }
})
