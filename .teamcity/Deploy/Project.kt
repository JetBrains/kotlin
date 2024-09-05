package Deploy

import Deploy.buildTypes.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("Deploy")
    name = "Deploy"

    buildType(DeployReleasePageDraftToGitHub)
    buildType(PrivacyManifestsPluginGradlePluginPortalPublication)
    buildType(DeployMavenArtifacts_Nightly_kotlin_space_packages)
    buildType(DeployGradlePluginIdea)
    buildType(DeployIdePluginDependenciesMavenArtifacts)
    buildType(PrivacyManifestsPluginPublication)
    buildType(PublishToGradlePluginPortalValidate)
    buildType(GitHubReleasePageDraftAggregate)
    buildType(DeployUserProjectsArtifacts)
    buildType(RelayFromSpaceToSonatype)
    buildType(PublishToNpm)
    buildType(DeployCompilerArtifactsToGitHub)
    buildType(DeployMavenArtifactsSonatypeSnapshot)
    buildType(KotlinNativePublishToS3)
    buildType(RelayNightlyDevToSonatype)
    buildType(KotlinNativePublish)
    buildType(CreateSonatypeStagingRepository)
    buildType(DeployMavenArtifacts)
    buildType(DeployMavenArtifacts_Bootstrap_kotlin_space_packages)
    buildType(KotlinNativePublishMaven)
    buildType(DeployKotlinMavenArtifacts)
    buildType(PublishMavenArtifactsToGitHub)
    buildTypesOrder = arrayListOf(DeployMavenArtifacts, DeployMavenArtifacts_Bootstrap_kotlin_space_packages, GitHubReleasePageDraftAggregate, DeployCompilerArtifactsToGitHub, DeployReleasePageDraftToGitHub, PublishMavenArtifactsToGitHub, DeployKotlinMavenArtifacts, DeployMavenArtifactsSonatypeSnapshot, DeployMavenArtifacts_Nightly_kotlin_space_packages, DeployUserProjectsArtifacts, PublishToNpm, DeployIdePluginDependenciesMavenArtifacts, DeployGradlePluginIdea, PrivacyManifestsPluginPublication, PrivacyManifestsPluginGradlePluginPortalPublication, PublishToGradlePluginPortalValidate, RelayNightlyDevToSonatype, RelayFromSpaceToSonatype, KotlinNativePublish, KotlinNativePublishMaven, KotlinNativePublishToS3, CreateSonatypeStagingRepository)
})
