package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KotlinxBenchmarkK2VCS : GitVcsRoot({
    name = "kotlinx.benchmark.k2"
    url = "git@github.com:Kotlin/kotlinx-benchmark.git"
    branch = "kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
