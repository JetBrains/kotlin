package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object IntellijK2VCS : GitVcsRoot({
    name = "intellij.k2"
    url = "ssh://git@git.jetbrains.team/intellij.git"
    branch = "kotlin-community/k2/dev"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
