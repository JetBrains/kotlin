package Aligners

import Aligners.buildTypes.*
import Aligners.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("Aligners")
    name = "Aligners"

    vcsRoot(IntellijKtMasterVCS)

    buildType(AlignSync)
    buildType(MainAggregateAligner)
    buildType(AlignDivergeCommitSearch)
    buildType(AlignCleanup)
    buildType(AlignPushLatest)
})
