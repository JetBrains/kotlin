package Tests_Linux.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object AtomicfuForAggregateVCS : GitVcsRoot({
    name = "kotlinx.atomicfu"
    url = "git@github.com:Kotlin/kotlinx-atomicfu.git"
    branch = "kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
