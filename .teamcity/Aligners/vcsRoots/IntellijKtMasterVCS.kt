package Aligners.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object IntellijKtMasterVCS : GitVcsRoot({
    name = "IntelliJ (kt-master)"
    url = "ssh://git@git.jetbrains.team/intellij.git"
    branch = "kt-master"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "jb.team -_ Applications -_ Kotlin Infrastructure Auto-Push"
    }
})
