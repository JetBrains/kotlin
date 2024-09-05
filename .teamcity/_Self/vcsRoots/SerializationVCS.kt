package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object SerializationVCS : GitVcsRoot({
    name = "kotlinx.serialization"
    url = "https://github.com/Kotlin/kotlinx.serialization.git"
    branch = "%branch.serialization%"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
