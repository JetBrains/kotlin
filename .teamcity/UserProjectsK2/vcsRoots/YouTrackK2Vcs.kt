package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object YouTrackK2Vcs : GitVcsRoot({
    name = "youtrack.k2"
    url = "ssh://git@git.jetbrains.team/yt/youtrack.git"
    branch = "kotlin-community/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
