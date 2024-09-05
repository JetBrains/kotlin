package Service.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object BuildFailureAnalyzer_1 : GitVcsRoot({
    id("BuildFailureAnalyzer")
    name = "Kotlin_BuildPlayground_Aquarius_BuildFailureAnalyzer"
    url = "ssh://git@git.jetbrains.team/kti/build-failure-analyzer.git"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
