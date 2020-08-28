/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.Project
import java.net.URI

var Project.bootstrapKotlinVersion: String
    get() = property("bootstrapKotlinVersion") as String
    private set(value) {
        extensions.extraProperties.set("bootstrapKotlinVersion", value)
    }

var Project.bootstrapKotlinRepo: String?
    get() = property("bootstrapKotlinRepo") as String?
    private set(value) {
        extensions.extraProperties.set("bootstrapKotlinRepo", value)
    }

@Deprecated("Obsolete, use internalBootstrapRepo instead.")
val Project.internalKotlinRepo: String?
    get() = "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinPublic_Compiler),number:$bootstrapKotlinVersion," +
            "branch:default:any/artifacts/content/internal/repo"

fun Project.kotlinBootstrapFrom(defaultSource: BootstrapOption) {
    val teamCityBootstrapVersion = kotlinBuildProperties.teamCityBootstrapVersion
    val customBootstrapVersion = kotlinBuildProperties.customBootstrapVersion
    val bootstrapSource = when {
        kotlinBuildProperties.localBootstrap -> BootstrapOption.Local(
            kotlinBuildProperties.localBootstrapVersion,
            kotlinBuildProperties.localBootstrapPath
        )
        teamCityBootstrapVersion != null -> BootstrapOption.TeamCity(
            teamCityBootstrapVersion,
            kotlinBuildProperties.teamCityBootstrapBuildNumber,
            projectExtId = kotlinBuildProperties.teamCityBootstrapProject,
            teamcityUrl = kotlinBuildProperties.teamCityBootstrapUrl,
            onlySuccessBootstrap = false
        )
        customBootstrapVersion != null -> BootstrapOption.Custom(
            kotlinVersion = customBootstrapVersion,
            repo = kotlinBuildProperties.customBootstrapRepo
        )
        else -> defaultSource
    }

    bootstrapSource.applyToProject(this)
    logger.lifecycle("Using kotlin bootstrap version $bootstrapKotlinVersion from repo $bootstrapKotlinRepo")
}

sealed class BootstrapOption {
    abstract fun applyToProject(project: Project)

    /** Manual repository and version specification.
     *
     *  If [repo] is not specified the default buildscript and project repositories are used
     */
    open class Custom(val kotlinVersion: String, val repo: String?, val cacheRedirector: Boolean = false) : BootstrapOption() {
        override fun applyToProject(project: Project) {
            project.bootstrapKotlinVersion = kotlinVersion
            project.bootstrapKotlinRepo = if (cacheRedirector)
                repo?.let { URI(it) }?.let { "https://cache-redirector.jetbrains.com/${it.host}/${it.path}" }
            else
                repo
        }
    }

    /** Get bootstrap from kotlin-dev bintray repo */
    class BintrayDev(kotlinVersion: String, cacheRedirector: Boolean = false) :
        Custom(kotlinVersion, "https://dl.bintray.com/kotlin/kotlin-dev", cacheRedirector)

    /** Get bootstrap from kotlin bootstrap space repo, where bootstraps are published */
    class BintrayBootstrap(kotlinVersion: String, cacheRedirector: Boolean = false) :
        Custom(kotlinVersion, "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap", cacheRedirector)

    /** Get bootstrap from teamcity maven artifacts of the specified build configuration
     *
     * [kotlinVersion] the version of maven artifacts
     * [buildNumber] build number of a teamcity build, by default the same as [kotlinVersion],
     * [projectExtId] extId of a teamcity build configuration, by default "Kotlin_dev_Compiler",
     * [onlySuccessBootstrap] allow artifacts only from success builds of the default branch tagged with 'bootstrap' tag
     */
    class TeamCity(
        val kotlinVersion: String,
        val buildNumber: String? = null,
        val projectExtId: String? = null,
        val onlySuccessBootstrap: Boolean = true,
        val teamcityUrl: String? = null
    ) : BootstrapOption() {
        override fun applyToProject(project: Project) {
            val query = if (onlySuccessBootstrap) "status:SUCCESS,tag:bootstrap,pinned:true" else "branch:default:any"
            project.bootstrapKotlinRepo =
                "${teamcityUrl ?: "https://buildserver.labs.intellij.net"}/guestAuth/app/rest/builds/buildType:(id:${projectExtId
                    ?: "Kotlin_KotlinDev_Compiler"}),number:${buildNumber ?: kotlinVersion},$query/artifacts/content/maven/"
            project.bootstrapKotlinVersion = kotlinVersion
        }
    }

    /**
     * Use previously published local artifacts from the build/repo maven repository
     *
     * [kotlinVersion] version of artifacts, by default the snapshot version of project is used
     * [localPath] the path to local repository, if specified it is resolved with respect or project dir
     */
    class Local(val kotlinVersion: String? = null, val localPath: String? = null) : BootstrapOption() {
        override fun applyToProject(project: Project) {
            val rootProjectDir = project.kotlinBuildProperties.rootProjectDir
            val repoPath = if (localPath != null)
                rootProjectDir.resolve(localPath).canonicalFile
            else
                rootProjectDir.resolve("build").resolve("repo")

            project.bootstrapKotlinRepo = repoPath.toURI().toString()
            project.bootstrapKotlinVersion = kotlinVersion ?: project.property("defaultSnapshotVersion") as String
        }
    }
}