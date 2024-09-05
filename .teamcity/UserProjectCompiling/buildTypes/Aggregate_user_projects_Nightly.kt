package UserProjectCompiling.buildTypes

import jetbrains.buildServer.configs.kotlin.*

object Aggregate_user_projects_Nightly : BuildType({
    name = "[Aggregate] All projects (NIGHTLY, artifacts from space packages)"
    description = "Night builds of user projects with the last published to space kotlin/dev repo"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "%reverse.dep.*.kotlin_snapshot_version%-%build.counter%"

    params {
        text("reverse.dep.*.kotlin_snapshot_version", "2.1.0", description = "see https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-compiler/", display = ParameterDisplay.PROMPT, allowEmpty = false)
    }

    dependencies {
        snapshot(Exposed) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(KotlinxTrainProjectK2.buildTypes.KotlinxTrainKtorK2) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(YouTrack) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(atomicfu) {
            reuseBuilds = ReuseBuilds.NO
        }
        snapshot(coroutines) {
            reuseBuilds = ReuseBuilds.NO
        }
    }
})
