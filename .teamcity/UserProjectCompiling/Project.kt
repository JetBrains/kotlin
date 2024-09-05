package UserProjectCompiling

import UserProjectCompiling.buildTypes.*
import UserProjectCompiling.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("UserProjectCompiling")
    name = "K1 user projects"
    description = "Building User projects with kotlin compiler"

    vcsRoot(kotlinxatomicfu)
    vcsRoot(kotlinxcoroutines_1)
    vcsRoot(YouTrack_1)

    buildType(BenchmarkCompilationCheckK1)
    buildType(Aggregate_user_projects_Nightly)
    buildType(atomicfu)
    buildType(Exposed)
    buildType(coroutines)
    buildType(YouTrack)
    buildType(UserProjectsAggregateK1)
    buildType(SerializationCompilationCheckK1)
    buildType(ktor)
})
