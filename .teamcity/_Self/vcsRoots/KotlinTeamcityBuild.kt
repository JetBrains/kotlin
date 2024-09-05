package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KotlinTeamcityBuild : GitVcsRoot({
    name = "kotlin-teamcity-build"
    url = "ssh://git@git.jetbrains.team/kti/kotlin-teamcity-build.git"
    branch = "playground/aquarius"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
