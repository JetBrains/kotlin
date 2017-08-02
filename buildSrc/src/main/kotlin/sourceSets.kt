@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.io.File
//import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

private fun Project.configureKotlinProjectSourceSet(srcs: Iterable<File>,
                                                    sourceSetName: String,
                                                    getSources: SourceSet.() -> SourceDirectorySet,
                                                    configureSourceDirs: SourceDirectorySet.() -> Unit) =
        configure<JavaPluginConvention> {
            //            if (srcs.none()) {
//                sourceSets.removeIf { it.name == sourceSetName }
//            }
//            else {
            sourceSets.matching { it.name == sourceSetName }.forEach { it.getSources().setSrcDirs(srcs).configureSourceDirs() }
//            }
        }

private fun Project.configureKotlinProjectSourceSet(vararg srcs: String, sourceSetName: String,
                                                    getSources: SourceSet.() -> SourceDirectorySet,
                                                    sourcesBaseDir: File? = null,
                                                    configureSourceDirs: SourceDirectorySet.() -> Unit = {}) =
        configureKotlinProjectSourceSet(srcs.map { File(sourcesBaseDir ?: projectDir, it) }, sourceSetName, getSources, configureSourceDirs)

fun Project.configureKotlinProjectSources(vararg srcs: String,
                                          sourcesBaseDir: File? = null,
                                          configureSourceDirs: SourceDirectorySet.() -> Unit = {}) =
        configureKotlinProjectSourceSet(*srcs, sourceSetName = "main", getSources = { this.java },
                sourcesBaseDir = sourcesBaseDir, configureSourceDirs = configureSourceDirs)

fun Project.configureKotlinProjectSources(srcs: Iterable<File>,
                                          configureSourceDirs: SourceDirectorySet.() -> Unit = {}) =
        configureKotlinProjectSourceSet(srcs, sourceSetName = "main", getSources = { this.java }, configureSourceDirs = configureSourceDirs)

fun Project.configureKotlinProjectSourcesDefault(sourcesBaseDir: File? = null,
                                                 configureSourceDirs: SourceDirectorySet.() -> Unit = {}) =
        configureKotlinProjectSources("src", sourcesBaseDir = sourcesBaseDir, configureSourceDirs = configureSourceDirs)

fun Project.configureKotlinProjectResources(vararg srcs: String,
                                            sourcesBaseDir: File? = null,
                                            configureSourceDirs: SourceDirectorySet.() -> Unit = {}) =
        configureKotlinProjectSourceSet(*srcs, sourceSetName = "main", getSources = { this.resources },
                sourcesBaseDir = sourcesBaseDir, configureSourceDirs = configureSourceDirs)

fun Project.configureKotlinProjectResources(srcs: Iterable<File>,
                                            configureSourceDirs: SourceDirectorySet.() -> Unit = {}) =
        configureKotlinProjectSourceSet(srcs, sourceSetName = "main", getSources = { this.resources }, configureSourceDirs = configureSourceDirs)

fun Project.configureKotlinProjectResourcesDefault(sourcesBaseDir: File? = null) {
    configureKotlinProjectResources("resources", sourcesBaseDir = sourcesBaseDir)
    configureKotlinProjectResources("src", sourcesBaseDir = sourcesBaseDir) { include("META-INF/**", "**/*.properties") }
}

fun Project.configureKotlinProjectNoTests() {
    configureKotlinProjectSourceSet(sourceSetName = "test", getSources = { this.java })
    configureKotlinProjectSourceSet(sourceSetName = "test", getSources = { this.resources })
}

fun Project.configureKotlinProjectTests(vararg srcs: String, sourcesBaseDir: File? = null,
                                        configureSourceDirs: SourceDirectorySet.() -> Unit = {}) =
        configureKotlinProjectSourceSet(*srcs, sourceSetName = "test", getSources = { this.java },
                sourcesBaseDir = sourcesBaseDir, configureSourceDirs = configureSourceDirs)

fun Project.configureKotlinProjectTestsDefault(sourcesBaseDir: File? = null,
                                               configureSourceDirs: SourceDirectorySet.() -> Unit = {}) =
        configureKotlinProjectTests("test", "tests", sourcesBaseDir = sourcesBaseDir, configureSourceDirs = configureSourceDirs)

fun Project.configureKotlinProjectTestResources(vararg srcs: String,
                                                sourcesBaseDir: File? = null,
                                                configureSourceDirs: SourceDirectorySet.() -> Unit = {}) =
        configureKotlinProjectSourceSet(*srcs, sourceSetName = "test", getSources = { this.resources },
                sourcesBaseDir = sourcesBaseDir, configureSourceDirs = configureSourceDirs)


inline fun Project.sourceSets(crossinline body: SourceSetsBuilder.() -> Unit) =
        SourceSetsBuilder(this).body()

class SourceSetsBuilder(val project: Project) {

    inline operator fun String.invoke(crossinline body: SourceSet.() -> Unit) {
        val sourceSetName = this
        project.configure<JavaPluginConvention>
        {
            sourceSets.matching { it.name == sourceSetName }.forEach {
                it.body()
            }
        }
    }
}

fun SourceSet.none() {
    java.srcDirs()
    resources.srcDirs()
}

fun SourceSet.default() {
    when (name) {
        "main" -> {
            java.srcDirs("src")
            resources.srcDir("resources")
            resources.srcDir("src").apply { include("META-INF/**", "**/*.properties") }
        }
        "test" -> {
            java.srcDirs("test", "tests")
        }
    }
}

// TODO: adding dep to the plugin breaks the build unexpectedly, resolve and uncomment
//val SourceSet.kotlin: SourceDirectorySet
//    get() =
//        (this as HasConvention)
//                .convention
//                .getPlugin(KotlinSourceSet::class.java)
//                .kotlin
//
//
//fun SourceSet.kotlin(action: SourceDirectorySet.() -> Unit) =
//        kotlin.action()
