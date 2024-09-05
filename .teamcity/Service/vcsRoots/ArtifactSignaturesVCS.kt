package Service.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object ArtifactSignaturesVCS : GitVcsRoot({
    name = "Kotlin_BuildPlayground_Aquarius_ArtifactSignaturesVCS"
    url = "ssh://git@git.jetbrains.team/kti/artifact-signatures.git"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
