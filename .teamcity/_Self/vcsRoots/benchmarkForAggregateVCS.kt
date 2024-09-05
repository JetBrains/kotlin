package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object benchmarkForAggregateVCS : GitVcsRoot({
    name = "kotlinx.benchmark"
    url = "git@github.com:Kotlin/kotlinx-benchmark.git"
    branch = "refs/heads/kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
