package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object ExposedVCS : GitVcsRoot({
    name = "exposed"
    url = "ssh://git@git.jetbrains.team/exposed_kct.git"
    branch = "kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
