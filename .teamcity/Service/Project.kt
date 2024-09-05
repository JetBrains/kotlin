package Service

import Service.buildTypes.*
import Service.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("Service")
    name = "Service"

    vcsRoot(ArtifactSignaturesVCS)
    vcsRoot(BranchAncestorAnalyzer_1)
    vcsRoot(BuildFailureAnalyzer_1)
    vcsRoot(PublishReleaseStatusCheckVCS)
    vcsRoot(AgentsConfigurationAssigner_1)

    buildType(BranchAncestorAnalyzer)
    buildType(QodanaMavenScan)
    buildType(BuildFailureAnalyzer)
    buildType(SynchronizeMutedTestsLINUX)
    buildType(AgentsCleanup)
    buildType(ArtifactSignatures)
    buildType(QodanaGradleScan)
    buildType(PublishReleaseStatusCheckBot)
    buildType(WindowsVMProvider)
    buildType(AgentsConfigurationAssigner)
    buildType(CompilerDiagnosticsIntroductionNotifier)
    buildType(AutoPush)
    buildType(BuildNumberTag)
})
