package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object ktorioktor : GitVcsRoot({
    name = "kotlinx.ktor.k1"
    url = "https://github.com/ktorio/ktor"
    branch = "%branch.ktor%"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
