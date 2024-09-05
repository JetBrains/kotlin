package UserProjectsK2.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object KotlinWasmWasiTemplatesK2VCS : GitVcsRoot({
    name = "KotlinWasmWasiTemplates.k2"
    url = "git@github.com:Kotlin/kotlin-wasm-wasi-template.git"
    branch = "kotlin-community/k2/dev"
    branchSpec = "+:refs/heads/(*)"
    authMethod = uploadedKey {
        uploadedKey = "default teamcity key"
    }
})
