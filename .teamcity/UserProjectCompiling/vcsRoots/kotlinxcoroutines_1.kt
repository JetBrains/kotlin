package UserProjectCompiling.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object kotlinxcoroutines_1 : GitVcsRoot({
    id("kotlinxcoroutines")
    name = "kotlinx.coroutines"
    url = "git@github.com:Kotlin/kotlinx.coroutines.git"
    branch = "refs/heads/kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
