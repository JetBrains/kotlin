package KotlinNativeTests.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object Kotlinx_Coroutines : GitVcsRoot({
    name = "kotlinx.coroutines"
    url = "https://github.com/Kotlin/kotlinx.coroutines"
    branch = "refs/heads/native-performance"
})
