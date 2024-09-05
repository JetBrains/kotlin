package UserProjectCompiling.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object coroutines : BuildType({
    name = "🐧 [Project] kotlinx.coroutines"
    description = "Build configuration: kotlinx.coroutines"

    artifactRules = "**/*.hprof=>internal/hprof.zip"
    buildNumberPattern = "%system.kotlin_snapshot_version%-%build.counter%"

    params {
        param("env.CACHE_REDIRECTOR", "true")
        param("branch.coroutines", "kotlin-community/dev")
        text("kotlin_snapshot_version", "", description = "see https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-compiler/", display = ParameterDisplay.PROMPT, allowEmpty = false)
        param("system.kotlin_snapshot_version", "%kotlin_snapshot_version%")
    }

    vcs {
        root(UserProjectCompiling.vcsRoots.kotlinxcoroutines_1, "+:.=>user-project")
    }

    steps {
        gradle {
            name = "build"
            tasks = "clean build"
            buildFile = "build.gradle.kts"
            workingDir = "user-project"
            gradleParams = "--stacktrace -x apiCheck -Pkotlin_language_version=1.9 -x knitCheck -x kotlinStoreYarnLock -Pkotlin_repo_url=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev --info --continue -Pkotlin_snapshot_version=%system.kotlin_snapshot_version% -Pkotlin_version=%system.kotlin_snapshot_version%"
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
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
