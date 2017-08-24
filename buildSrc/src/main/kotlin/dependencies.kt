@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.kotlin.dsl.*
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec
import java.io.File
import java.util.*

val bootstrapKotlinVersion: String = System.getProperty("bootstrap.kotlin.version") ?: embeddedKotlinVersion

fun PluginDependenciesSpec.kotlin(module: String, version: String? = null): PluginDependencySpec =
        id("org.jetbrains.kotlin.$module") version (version ?: bootstrapKotlinVersion)

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

fun Project.preloadedDeps(vararg artifactBaseNames: String, baseDir: File = File(rootDir, "dependencies"), subdir: String? = null): ConfigurableFileCollection {
    val dir = if (subdir != null) File(baseDir, subdir) else baseDir
    if (!dir.exists() || !dir.isDirectory) throw GradleException("Invalid base directory $dir")
    val matchingFiles = dir.listFiles { file -> artifactBaseNames.any { file.matchMaybeVersionedArtifact(it) } }
    if (matchingFiles == null || matchingFiles.size < artifactBaseNames.size)
        throw GradleException("Not all matching artifacts '${artifactBaseNames.joinToString()}' found in the '$dir' (found: ${matchingFiles?.joinToString { it.name }})")
    return files(*matchingFiles.map { it.canonicalPath }.toTypedArray())
}

fun Project.ideaSdkDeps(vararg artifactBaseNames: String, subdir: String = "lib"): ConfigurableFileCollection =
        preloadedDeps(*artifactBaseNames, baseDir = File(rootDir, "ideaSDK"), subdir = subdir)

fun Project.ideaSdkCoreDeps(vararg artifactBaseNames: String): ConfigurableFileCollection = ideaSdkDeps(*artifactBaseNames, subdir = "core")

fun Project.ideaPluginDeps(vararg artifactBaseNames: String, plugin: String, subdir: String = "lib"): ConfigurableFileCollection =
        preloadedDeps(*artifactBaseNames, baseDir = File(rootDir, "ideaSDK"), subdir = "plugins/$plugin/$subdir")

fun Project.kotlinDep(artifactBaseName: String, version: String? = null): String = "org.jetbrains.kotlin:kotlin-$artifactBaseName:${version ?: bootstrapKotlinVersion}"

fun DependencyHandler.projectDep(name: String): Dependency = project(name, configuration = "default")
fun DependencyHandler.projectDepIntransitive(name: String): Dependency =
        project(name, configuration = "default").apply { isTransitive = false }

fun DependencyHandler.projectDist(name: String): Dependency = project(name, configuration = "distJar").apply { isTransitive = false }
fun DependencyHandler.projectTests(name: String): Dependency = project(name, configuration = "tests-jar").apply { isTransitive = false }
fun DependencyHandler.projectRuntimeJar(name: String): Dependency = project(name, configuration = "runtimeJar")
fun DependencyHandler.projectArchives(name: String): Dependency = project(name, configuration = "archives")

val protobufLiteProject = ":custom-dependencies:protobuf-lite"
fun DependencyHandler.protobufLite(): ProjectDependency =
        project(protobufLiteProject, configuration = "default").apply { isTransitive = false }
val protobufLiteTask = "$protobufLiteProject:prepare"

fun DependencyHandler.protobufFull(): ProjectDependency =
        project(protobufLiteProject, configuration = "relocated").apply { isTransitive = false }
val protobufFullTask = "$protobufLiteProject:prepare-relocated-protobuf"

private fun File.matchMaybeVersionedArtifact(baseName: String) = name.matches(baseName.toMaybeVersionedJarRegex())

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

