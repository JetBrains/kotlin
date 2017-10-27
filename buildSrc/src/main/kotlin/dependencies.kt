@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.project
import java.io.File


fun Project.commonDep(coord: String): String {
    val parts = coord.split(':')
    return when (parts.size) {
        1 -> "$coord:$coord:${rootProject.extra["versions.$coord"]}"
        2 -> "${parts[0]}:${parts[1]}:${rootProject.extra["versions.${parts[1]}"]}"
        3 -> coord
        else -> throw IllegalArgumentException("Illegal maven coordinates: $coord")
    }
}

fun Project.commonDep(group: String, artifact: String): String = "$group:$artifact:${rootProject.extra["versions.$artifact"]}"

fun Project.preloadedDeps(vararg artifactBaseNames: String, baseDir: File = File(rootDir, "dependencies"), subdir: String? = null, optional: Boolean = false): ConfigurableFileCollection {
    val dir = if (subdir != null) File(baseDir, subdir) else baseDir
    if (!dir.exists() || !dir.isDirectory) {
        if (optional) return files()
        throw GradleException("Invalid base directory $dir")
    }
    val matchingFiles = dir.listFiles { file -> artifactBaseNames.any { file.matchMaybeVersionedArtifact(it) } }
    if (matchingFiles == null || matchingFiles.size < artifactBaseNames.size) {
        throw GradleException("Not all matching artifacts '${artifactBaseNames.joinToString()}' found in the '$dir' " +
                              "(missing: ${artifactBaseNames.filterNot { request -> matchingFiles.any { it.matchMaybeVersionedArtifact(request) } }.joinToString()};" +
                              " found: ${matchingFiles?.joinToString { it.name }})")
    }
    return files(*matchingFiles.map { it.canonicalPath }.toTypedArray())
}

fun Project.ideaUltimatePreloadedDeps(vararg artifactBaseNames: String, subdir: String? = null): ConfigurableFileCollection {
    val ultimateDepsDir = File(rootDir, "ultimate", "dependencies")
    return if (ultimateDepsDir.isDirectory) preloadedDeps(*artifactBaseNames, baseDir = ultimateDepsDir, subdir = subdir)
    else files()
}

fun Project.ideaSdkDeps(vararg artifactBaseNames: String, subdir: String = "lib", optional: Boolean = false): ConfigurableFileCollection =
        preloadedDeps(*artifactBaseNames, baseDir = File(rootDir, "ideaSDK"), subdir = subdir, optional = optional)

fun Project.ideaUltimateSdkDeps(vararg artifactBaseNames: String, subdir: String = "lib"): ConfigurableFileCollection {
    val ultimateSdkDir = File(rootDir, "ultimate", "ideaSDK")
    return if (ultimateSdkDir.isDirectory) preloadedDeps(*artifactBaseNames, baseDir = ultimateSdkDir, subdir = subdir)
           else files()
}

fun Project.ideaSdkCoreDeps(vararg artifactBaseNames: String): ConfigurableFileCollection = ideaSdkDeps(*artifactBaseNames, subdir = "core")

fun Project.ideaUltimateSdkCoreDeps(vararg artifactBaseNames: String): ConfigurableFileCollection = ideaUltimateSdkDeps(*artifactBaseNames, subdir = "core")

fun Project.ideaPluginDeps(vararg artifactBaseNames: String, plugin: String, subdir: String = "lib", optional: Boolean = false): ConfigurableFileCollection =
        ideaSdkDeps(*artifactBaseNames, subdir = "plugins/$plugin/$subdir", optional = optional)

fun Project.ideaUltimatePluginDeps(vararg artifactBaseNames: String, plugin: String, subdir: String = "lib"): ConfigurableFileCollection =
        ideaUltimateSdkDeps(*artifactBaseNames, subdir = "plugins/$plugin/$subdir")

fun Project.kotlinDep(artifactBaseName: String, version: String): String = "org.jetbrains.kotlin:kotlin-$artifactBaseName:$version"

fun DependencyHandler.projectDist(name: String): ProjectDependency = project(name, configuration = "distJar").apply { isTransitive = false }
fun DependencyHandler.projectTests(name: String): ProjectDependency = project(name, configuration = "tests-jar")
fun DependencyHandler.projectRuntimeJar(name: String): ProjectDependency = project(name, configuration = "runtimeJar")
fun DependencyHandler.projectArchives(name: String): ProjectDependency = project(name, configuration = "archives")
fun DependencyHandler.projectClasses(name: String): ProjectDependency = project(name, configuration = "classes-dirs")

val protobufLiteProject = ":custom-dependencies:protobuf-lite"
fun DependencyHandler.protobufLite(): ProjectDependency =
        project(protobufLiteProject, configuration = "default").apply { isTransitive = false }
val protobufLiteTask = "$protobufLiteProject:prepare"

fun DependencyHandler.protobufFull(): ProjectDependency =
        project(protobufLiteProject, configuration = "relocated").apply { isTransitive = false }
val protobufFullTask = "$protobufLiteProject:prepare-relocated-protobuf"

fun File.matchMaybeVersionedArtifact(baseName: String) = name.matches(baseName.toMaybeVersionedJarRegex())

private val wildcardsRe = """[^*?]+|(\*)|(\?)""".toRegex()

private fun String.wildcardsToEscapedRegexString(): String = buildString {
    wildcardsRe.findAll(this@wildcardsToEscapedRegexString).forEach {
        when {
            it.groups[1] != null -> append(".*")
            it.groups[2] != null -> append(".")
            else -> append("\\Q${it.groups[0]!!.value}\\E")
        }
    }
}

private fun String.toMaybeVersionedJarRegex(): Regex {
    val hasJarExtension = endsWith(".jar")
    val escaped = this.wildcardsToEscapedRegexString()
    return Regex(if (hasJarExtension) escaped else "$escaped(-\\d.*)?\\.jar") // TODO: consider more precise version part of the regex
}


private val jreHome = System.getProperty("java.home")

fun firstFromJavaHomeThatExists(vararg paths: String): File =
        paths.mapNotNull { File(jreHome, it).takeIf { it.exists() } }.firstOrNull()
                ?: throw GradleException("Cannot find under '$jreHome' neither of: ${paths.joinToString()}")

fun toolsJar(): File = firstFromJavaHomeThatExists("../lib/tools.jar", "../Classes/tools.jar")
