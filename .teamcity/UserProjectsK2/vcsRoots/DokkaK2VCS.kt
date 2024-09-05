package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object DokkaK2VCS : GitVcsRoot({
    name = "dokka_kct kct"
    url = "ssh://git@git.jetbrains.team/kct/dokka_kct.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
