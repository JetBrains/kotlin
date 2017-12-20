@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.kotlin.dsl.extra
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository

private fun Project.intellijRepoDir() = File("${project.rootDir.absoluteFile}/buildSrc/prepare-deps/intellij-sdk/build/repo")

fun RepositoryHandler.intellijSdkRepo(project: Project): IvyArtifactRepository = ivy {
    val baseDir = project.intellijRepoDir()
    setUrl(baseDir)
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module].ivy.xml")
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellij.plugin.[module].ivy.xml")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellij/plugins/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/plugins-[module]/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/[artifact].jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/[artifact](-[revision])(-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/sources/[artifact]-[revision]-[classifier].[ext]")
}

fun Project.intellijDep(module: String = "intellij") = "kotlin.build.custom.deps:$module:${rootProject.extra["versions.intellijSdk"]}"

fun Project.intellijCoreDep() = intellijDep("intellij-core")

fun Project.intellijPluginDep(plugin: String) = intellijDep(plugin)

fun ModuleDependency.includeJars(vararg names: String) {
    names.forEach {
        artifact {
            name = it.removeSuffix(".jar")
            type = "jar"
            extension = "jar"
        }
    }
}

fun ModuleDependency.includeIntellijCoreJarDependencies(project: Project) =
        includeJars(*(project.rootProject.extra["IntellijCoreDependenciesJars"] as List<String>).toTypedArray())

fun ModuleDependency.includeIntellijCoreJarDependencies(project: Project, jarsFilterPredicate: (String) -> Boolean) =
        includeJars(*(project.rootProject.extra["IntellijCoreDependenciesJars"] as List<String>).filter { jarsFilterPredicate(it) }.toTypedArray())

fun Project.intellijRootDir() = File(intellijRepoDir(), "kotlin.build.custom.deps/${rootProject.extra["versions.intellijSdk"]}/intellij")

