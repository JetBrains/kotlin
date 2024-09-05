package Service.buildTypes

import _Self.buildTypes.Aggregate
import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

object BuildFailureAnalyzer : BuildType({
    name = "🐧 Build Failure Analyzer"

    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("gradleParameters", "%globalGradleParameters%")
    }

    vcs {
        root(Service.vcsRoots.BuildFailureAnalyzer_1, "+:. => build-failure-analyzer")
    }

    steps {
        gradle {
            name = "run analyzer task"
            tasks = ":run"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/build-failure-analyzer"
            gradleParams = "%gradleParameters% --parallel -Ptoken=%teamcity.serviceUser.token% -PbuildId=${Aggregate.depParamRefs["teamcity.build.id"]}"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${Aggregate.id}"
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.Aggregate) {
            reuseBuilds = ReuseBuilds.NO
            onDependencyFailure = FailureAction.IGNORE
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
