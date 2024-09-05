package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object CommunityProjectPluginVcs : GitVcsRoot({
    name = "Community project plugin VCS"
    url = "ssh://git@git.jetbrains.team/kti/community-project-plugin.git"
    branch = "refs/tags/1.1"
    branchSpec = """
        +:refs/heads/(*)
        +:refs/tags/(*)
    """.trimIndent()
    useTagsAsBranches = true
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
