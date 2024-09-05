package Service.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object BranchAncestorAnalyzer_1 : GitVcsRoot({
    id("BranchAncestorAnalyzer")
    name = "Kotlin_BuildPlayground_Aquarius_BranchAncestorAnalyzer"
    url = "ssh://git@git.jetbrains.team/kti/build-failure-analyzer.git"
    branch = "master"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
