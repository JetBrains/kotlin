@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.AbstractCopyTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.project
import java.io.File

val Project.isSnapshotIntellij get() = rootProject.extra["versions.intellijSdk"].toString().endsWith("SNAPSHOT")

val Project.intellijRepo get() = "https://www.jetbrains.com/intellij-repository/" + if (isSnapshotIntellij) "snapshots" else "releases"

fun Project.commonDep(coord: String): String {
    val parts = coord.split(':')
    return when (parts.size) {
        1 -> "$coord:$coord:${commonVer(coord, coord)}"
        2 -> "${parts[0]}:${parts[1]}:${commonVer(parts[0], parts[1])}"
        3 -> coord
        else -> throw IllegalArgumentException("Illegal maven coordinates: $coord")
    }
}

fun Project.commonDep(group: String, artifact: String, vararg suffixesAndClassifiers: String): String {
    val (classifiers, artifactSuffixes) = suffixesAndClassifiers.partition { it.startsWith(':') }
    return "$group:$artifact${artifactSuffixes.joinToString("")}:${commonVer(group, artifact)}${classifiers.joinToString("")}"
}

fun Project.commonVer(group: String, artifact: String) =
    when {
        rootProject.extra.has("versions.$artifact") -> rootProject.extra["versions.$artifact"]
        rootProject.extra.has("versions.$group") -> rootProject.extra["versions.$group"]
        else -> throw GradleException("Neither versions.$artifact nor versions.$group is defined in the root project's extra")
    }

fun Project.preloadedDeps(
    vararg artifactBaseNames: String,
    baseDir: File = File(rootDir, "dependencies"),
    subdir: String? = null,
    optional: Boolean = false
): ConfigurableFileCollection {
    val dir = if (subdir != null) File(baseDir, subdir) else baseDir
    if (!dir.exists() || !dir.isDirectory) {
        if (optional) return files()
        throw GradleException("Invalid base directory $dir")
    }
    val matchingFiles = dir.listFiles { file -> artifactBaseNames.any { file.matchMaybeVersionedArtifact(it) } }
    if (matchingFiles == null || matchingFiles.size < artifactBaseNames.size) {
        throw GradleException(
            "Not all matching artifacts '${artifactBaseNames.joinToString()}' found in the '$dir' " +
                    "(missing: ${artifactBaseNames.filterNot { request ->
                        matchingFiles.any {
                            it.matchMaybeVersionedArtifact(
                                request
                            )
                        }
                    }.joinToString()};" +
                    " found: ${matchingFiles?.joinToString { it.name }})"
        )
    }
    return files(*matchingFiles.map { it.canonicalPath }.toTypedArray())
}

fun Project.ideaUltimatePreloadedDeps(vararg artifactBaseNames: String, subdir: String? = null): ConfigurableFileCollection {
    val ultimateDepsDir = fileFrom(rootDir, "ultimate", "dependencies")
    return if (ultimateDepsDir.isDirectory) preloadedDeps(*artifactBaseNames, baseDir = ultimateDepsDir, subdir = subdir)
    else files()
}

fun Project.kotlinDep(artifactBaseName: String, version: String): String = "org.jetbrains.kotlin:kotlin-$artifactBaseName:$version"

val Project.useBootstrapStdlib: Boolean get() =
    findProperty("jpsBuild")?.toString() == "true"

fun Project.kotlinStdlib(suffix: String? = null): Any {
    return if (useBootstrapStdlib)
        kotlinDep(listOfNotNull("stdlib", suffix).joinToString("-"), bootstrapKotlinVersion)
    else
        dependencies.project(listOfNotNull(":kotlin-stdlib", suffix).joinToString("-"))
}

fun DependencyHandler.projectTests(name: String): ProjectDependency = project(name, configuration = "tests-jar")
fun DependencyHandler.projectRuntimeJar(name: String): ProjectDependency = project(name, configuration = "runtimeJar")
fun DependencyHandler.projectArchives(name: String): ProjectDependency = project(name, configuration = "archives")

val Project.protobufVersion: String get() = findProperty("versions.protobuf") as String

val Project.protobufRepo: String
    get() =
        "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_Protobuf),status:SUCCESS,pinned:true,tag:$protobufVersion/artifacts/content/internal/repo/"

fun Project.protobufLite(): String = "org.jetbrains.kotlin:protobuf-lite:$protobufVersion"
fun Project.protobufFull(): String = "org.jetbrains.kotlin:protobuf-relocated:$protobufVersion"

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


fun Project.firstFromJavaHomeThatExists(vararg paths: String, jdkHome: File = File(this.property("JDK_18") as String)): File? =
    paths.map { File(jdkHome, it) }.firstOrNull { it.exists() }.also {
        if (it == null)
            logger.warn("Cannot find file by paths: ${paths.toList()} in $jdkHome")
    }

fun Project.toolsJar(jdkHome: File = File(this.property("JDK_18") as String)): File? =
    firstFromJavaHomeThatExists("lib/tools.jar", jdkHome = jdkHome)

val compilerManifestClassPath
    get() = "annotations-13.0.jar kotlin-stdlib.jar kotlin-reflect.jar kotlin-script-runtime.jar trove4j.jar"

object EmbeddedComponents {
    val CONFIGURATION_NAME = "embeddedComponents"
}

fun AbstractCopyTask.fromEmbeddedComponents() {
    val embeddedComponents = project.configurations.getByName(EmbeddedComponents.CONFIGURATION_NAME)
    if (this is ShadowJar) {
        from(embeddedComponents)
    } else {
        dependsOn(embeddedComponents)
        from {
            embeddedComponents.map { file ->
                if (file.isDirectory)
                    project.files(file)
                else
                    project.zipTree(file)
            }
        }
    }
}
