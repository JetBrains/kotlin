package KotlinNative.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object Kotlin_Native_IOS_Upload_Test : GitVcsRoot({
    name = "Kotlin iOS Upload Test"
    url = "ssh://git.jetbrains.team/kotlin-ios-upload-test/kotlin-ios-upload-test.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "git.jetbrains.team"
    }
})
