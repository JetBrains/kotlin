@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*


var Project.bootstrapKotlinVersion: String
    get() = this.property("bootstrapKotlinVersion") as String
    private set(value) { this.extra["bootstrapKotlinVersion"] = value }
var Project.bootstrapKotlinRepo: String?
    get() = this.property("bootstrapKotlinRepo") as String?
    private set(value) { this.extra["bootstrapKotlinRepo"] = value }

fun Project.kotlinBootstrapFrom(defaultSource: BootstrapOption) {
    val customVersion = project.findProperty("bootstrap.kotlin.version") as String?
    val customRepo = project.findProperty("bootstrap.kotlin.repo") as String?
    val teamCityVersion = project.findProperty("bootstrap.teamcity.kotlin.version") as String?
    val teamCityBuild = project.findProperty("bootstrap.teamcity.build.number") as String?
    val teamCityProject = project.findProperty("bootstrap.teamcity.project") as String?

    val bootstrapSource = when {
        project.hasProperty("bootstrap.local") -> BootstrapOption.Local(project.findProperty("bootstrap.local.version") as String?, project.findProperty("bootstrap.local.path") as String?)
        teamCityVersion != null -> BootstrapOption.TeamCity(teamCityVersion, teamCityBuild, projectExtId = teamCityProject, onlySuccessBootstrap = false)
        customVersion != null -> BootstrapOption.Custom(kotlinVersion = customVersion, repo = customRepo)
        else -> defaultSource
    }

    bootstrapSource.applyToProject(project)
    project.logger.lifecycle("Using kotlin bootstrap version $bootstrapKotlinVersion from repo $bootstrapKotlinRepo")
}

sealed class BootstrapOption {
    abstract fun applyToProject(project: Project)

    /** Manual repository and version specification.
     *
     *  If [repo] is not specified the default buildscript and project repositories are used
     */
    open class Custom(val kotlinVersion: String, val repo: String?) : BootstrapOption() {
        override fun applyToProject(project: Project) {
            project.bootstrapKotlinVersion = kotlinVersion
            project.bootstrapKotlinRepo = repo
        }
    }

    /** Get bootstrap from kotlin-dev bintray repo, where bootstraps are published */
    class BintrayDev(kotlinVersion: String) : Custom(kotlinVersion, "https://dl.bintray.com/kotlin/kotlin-dev")

    /** Get bootstrap from teamcity maven artifacts of the specified build configuration
     *
     * [kotlinVersion] the version of maven artifacts
     * [buildNumber] build number of a teamcity build, by default the same as [kotlinVersion],
     * [projectExtId] extId of a teamcity build configuration, by default "Kotlin_dev_Compiler",
     * [onlySuccessBootstrap] allow artifacts only from success builds of the default branch tagged with 'bootstrap' tag
     */
    class TeamCity(val kotlinVersion: String, val buildNumber: String? = null, val projectExtId: String? = null, val onlySuccessBootstrap: Boolean = true) : BootstrapOption() {
        override fun applyToProject(project: Project) {
            val query = if (onlySuccessBootstrap) "status:SUCCESS,tag:bootstrap,pinned:true" else "branch:default:any"
            project.bootstrapKotlinRepo = "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:${projectExtId ?: "Kotlin_dev_Compiler"}),number:${buildNumber ?: kotlinVersion},$query/artifacts/content/maven/"
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
            val repoPath = if (localPath != null)
                project.projectDir.resolve(localPath).canonicalFile
            else
                project.buildDir.resolve("repo")

            project.bootstrapKotlinRepo = repoPath.toURI().toString()
            project.bootstrapKotlinVersion = kotlinVersion ?: project.property("defaultSnapshotVersion") as String
        }
    }
}