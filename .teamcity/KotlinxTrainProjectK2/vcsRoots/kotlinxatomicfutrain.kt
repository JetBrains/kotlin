package KotlinxTrainProjectK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object kotlinxatomicfutrain : GitVcsRoot({
    name = "kotlinx atomicfu train"
    url = "https://github.com/Kotlin/kotlinx.atomicfu.git"
    branch = "refs/heads/%branch.atomicfu%"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
