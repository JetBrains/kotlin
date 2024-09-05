package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KotlinxAtomicFuK2VCS : GitVcsRoot({
    name = "kotlinx.atomicfu.k2"
    url = "git@github.com:Kotlin/kotlinx-atomicfu.git"
    branch = "kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
