package Service.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object AgentsConfigurationAssigner_1 : GitVcsRoot({
    id("AgentsConfigurationAssigner")
    name = "Kotlin_BuildPlayground_Aquarius_AgentsConfigurationAssigner"
    url = "ssh://git@git.jetbrains.team/kti/agents-configuration-assigner.git"
    branch = "master"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
