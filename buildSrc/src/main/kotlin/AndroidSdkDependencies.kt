@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository

fun RepositoryHandler.androidDxJarRepo(project: Project): IvyArtifactRepository = ivy {
    artifactPattern("${project.rootDir.absoluteFile.toURI().toURL()}/buildSrc/prepare-deps/android-dx/build/libs/[artifact](-[classifier]).jar")
}

fun androidDxJar() = "my-custom-deps:dx:0"
