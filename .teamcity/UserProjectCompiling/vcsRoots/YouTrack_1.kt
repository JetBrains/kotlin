package UserProjectCompiling.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object YouTrack_1 : GitVcsRoot({
    id("YouTrack")
    name = "YouTrack"
    url = "ssh://git@git.jetbrains.team/yt/youtrack.git"
    branch = "refs/heads/%branch.youtrack%"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
