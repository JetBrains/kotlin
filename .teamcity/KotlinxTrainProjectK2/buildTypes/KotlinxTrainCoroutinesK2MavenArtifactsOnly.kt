package KotlinxTrainProjectK2.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object KotlinxTrainCoroutinesK2MavenArtifactsOnly : BuildType({
    name = "🍏ᵐ Kotlinx.coroutines K2 MacOS build and publish to MavenLocal"
    description = "This step produces Coroutines MacOs artifacts which are passed to Ktor on the next step"

    artifactRules = """
        %train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx
        **/*.hprof=>internal/hprof.zip
    """.trimIndent()
    buildNumberPattern = "%DeployVersion% (%build.counter%)"

    params {
        param("train.maven.local.repository.path", "%teamcity.build.checkoutDir%/%train.maven.local.repository.dir%")
        param("DeployVersion", "2.2.3-train-%kotlin_snapshot_version%")
        param("branch.coroutines", "kotlin-community/k2/dev")
        param("kotlin_snapshot_version", "")
        param("train.maven.local.repository.dir", ".localrepo")
    }

    vcs {
        root(_Self.vcsRoots.KotlinxCoroutinesK2VCS, "+:. => project.coroutines")
    }

    steps {
        gradle {
            name = "Publish coroutines to maven local"
            tasks = "publishToMavenLocal"
            buildFile = "build.gradle.kts"
            workingDir = "project.coroutines"
            gradleParams = """
                --stacktrace 
                -x check -x test
                -x kotlinStoreYarnLock
                -Patomicfu_version=%DeployVersion%
                -Pkotlin_version=%kotlin_snapshot_version%  
                -Pkotlin_snapshot_version=%kotlin_snapshot_version%    
                -Pskip_snapshot_checks=true
                -Pkotlin_language_version=2.0  
                -Dmaven.repo.local=%train.maven.local.repository.path%
                -Pkotlin_repo_url=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev --info 
                -Pbuild_snapshot_train=true
                -PDeployVersion=%DeployVersion%
                --continue
            """.trimIndent()
            jdkHome = "%env.JDK_11%"
        }
    }

    failureConditions {
        executionTimeoutMin = 30
        supportTestRetry = true
    }

    dependencies {
        dependency(KotlinxTrainAtomicfuK2) {
            snapshot {
            }

            artifacts {
                artifactRules = "%train.maven.local.repository.dir%/org/jetbrains/kotlinx/**=>%train.maven.local.repository.dir%/org/jetbrains/kotlinx"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        startsWith("teamcity.agent.name", "kotlin-macos")
        moreThan("teamcity.agent.hardware.memorySizeMb", "6000")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "4000")
    }
})
