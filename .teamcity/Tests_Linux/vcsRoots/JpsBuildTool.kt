package Tests_Linux.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object JpsBuildTool : GitVcsRoot({
    name = "Kotlin_BuildPlayground_Aquarius_JpsBuildTool"
    url = "https://github.com/JetBrains/idea-gradle-jps-build-app.git"
    branch = "idea-211-portable-caches"
    branchSpec = "+:refs/heads/(idea-211-portable-caches)"
})
