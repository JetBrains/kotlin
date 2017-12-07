@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.kotlin.dsl.extra

fun RepositoryHandler.androidDxJarRepo(project: Project): IvyArtifactRepository = ivy {
    val baseDir = File("${project.rootDir}/buildSrc/prepare-deps/android-dx/build/repo")
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[module]/[revision]/[module].ivy.xml")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[module]/[revision]/[artifact](-[classifier]).jar")
}

fun Project.androidDxJar() = "kotlin.build.custom.deps:android-dx:${rootProject.extra["versions.androidBuildTools"]}"
