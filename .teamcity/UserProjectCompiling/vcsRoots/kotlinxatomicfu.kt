package UserProjectCompiling.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object kotlinxatomicfu : GitVcsRoot({
    name = "kotlinx.atomicfu"
    url = "https://github.com/Kotlin/kotlinx.atomicfu.git"
    branch = "refs/heads/%branch.atomicfu%"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
