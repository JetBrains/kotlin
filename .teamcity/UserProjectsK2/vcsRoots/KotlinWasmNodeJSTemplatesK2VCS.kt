package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KotlinWasmNodeJSTemplatesK2VCS : GitVcsRoot({
    name = "KotlinWasmNodeJSTemplates.k2"
    url = "git@github.com:Kotlin/kotlin-wasm-nodejs-template.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
