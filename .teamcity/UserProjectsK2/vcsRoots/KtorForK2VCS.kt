package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KtorForK2VCS : GitVcsRoot({
    name = "ktorio.ktor.k2 (in progress)"
    url = "https://github.com/ktorio/ktor"
    branch = "kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
