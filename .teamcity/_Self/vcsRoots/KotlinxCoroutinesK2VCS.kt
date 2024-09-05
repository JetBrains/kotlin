package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KotlinxCoroutinesK2VCS : GitVcsRoot({
    name = "kotlinx.coroutines.k2"
    url = "git@github.com:Kotlin/kotlinx.coroutines.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
