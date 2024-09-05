package Service.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object PublishReleaseStatusCheckVCS : GitVcsRoot({
    name = "Kotlin_BuildPlayground_Aquarius_PublishReleaseStatusCheckVCS"
    url = "ssh://git@git.jetbrains.team/kqa/aggregate-bot.git"
    branch = "master"
    authMethod = uploadedKey {
        uploadedKey = "git.jetbrains.team"
    }
})
