package _Self.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object IntellijMonorepoForKotlinVCS_kt_master : GitVcsRoot({
    name = "IntelliJ (kt-master) with aligner references"
    url = "ssh://git@git.jetbrains.team/intellij.git"
    branch = "kt-master"
    branchSpec = """
        +:refs/heads/(*)
        +:refs/buildserver/kotlin-align/kt-master/(*)
    """.trimIndent()
    authMethod = uploadedKey {
        uploadedKey = "jb.team -_ Applications -_ Kotlin Infrastructure Auto-Push"
    }
})
