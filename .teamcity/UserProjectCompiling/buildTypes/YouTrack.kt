package UserProjectCompiling.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object YouTrack : BuildType({
    name = "🐧 [Project] YouTrack"
    description = "original repo: ssh://git@git.jetbrains.team/yt/youtrack.git"

    artifactRules = "**/*.hprof=>internal/hprof.zip"
    buildNumberPattern = "%system.kotlin_snapshot_version%-%build.counter%"

    params {
        param("branch.youtrack", "kotlin-community/k1/dev")
        text("kotlin_snapshot_version", "", description = "see https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-compiler/", display = ParameterDisplay.PROMPT, allowEmpty = false)
        param("system.kotlin_snapshot_version", "%kotlin_snapshot_version%")
    }

    vcs {
        root(UserProjectCompiling.vcsRoots.YouTrack_1, "+:.=>user-project")
    }

    steps {
        gradle {
            name = "build"
            tasks = "clean jar"
            buildFile = "build.gradle"
            workingDir = "user-project"
            gradleParams = "--stacktrace -Pkotlin_language_version=1.9 -Pkotlin_version=%system.kotlin_snapshot_version%"
            jdkHome = "%env.JDK_17_0%"
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
