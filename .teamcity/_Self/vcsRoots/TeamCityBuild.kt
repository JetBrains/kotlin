package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object TeamCityBuild : GitVcsRoot({
    name = "TeamCity Build"
    url = "ssh://git@git.jetbrains.team/kotlin-teamcity-build.git"
    branch = "refs/heads/playground/aquarius"
    authMethod = defaultPrivateKey {
    }
})
