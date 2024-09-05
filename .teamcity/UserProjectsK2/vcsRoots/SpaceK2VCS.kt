package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object SpaceK2VCS : GitVcsRoot({
    name = "space.k2"
    url = "ssh://git@git.jetbrains.team/crl/space.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
