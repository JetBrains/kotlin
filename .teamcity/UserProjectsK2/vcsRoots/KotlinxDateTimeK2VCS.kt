package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KotlinxDateTimeK2VCS : GitVcsRoot({
    name = "kotlinx.datetime.k2"
    url = "git@github.com:Kotlin/kotlinx-datetime.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
