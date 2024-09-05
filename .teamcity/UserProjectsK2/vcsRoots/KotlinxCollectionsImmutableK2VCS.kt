package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KotlinxCollectionsImmutableK2VCS : GitVcsRoot({
    name = "kotlinx.collections.immutable.k2"
    url = "git@github.com:Kotlin/kotlinx.collections.immutable.git"
    branch = "kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
