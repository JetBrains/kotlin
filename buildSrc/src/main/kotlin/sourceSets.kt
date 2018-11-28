@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

//import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

inline fun Project.sourceSets(crossinline body: SourceSetsBuilder.() -> Unit) =
        SourceSetsBuilder(this).body()

class SourceSetsBuilder(val project: Project) {

    inline operator fun String.invoke(crossinline body: SourceSet.() -> Unit): SourceSet {
        val sourceSetName = this
        return project.sourceSets.maybeCreate(sourceSetName).apply {
            none()
            body()
        }
    }
}

fun SourceSet.none() {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

val SourceSet.projectDefault: Project.() -> Unit
    get() = {
        when (this@projectDefault.name) {
            "main" -> {
                java.srcDirs("src")
                val processResources = tasks.getByName(processResourcesTaskName) as ProcessResources
                processResources.from("resources") { include("**") }
                processResources.from("src") { include("META-INF/**", "**/*.properties") }
            }
            "test" -> {
                java.srcDirs("test", "tests")
            }
        }
    }

// TODO: adding KotlinSourceSet dep to the plugin breaks the build unexpectedly, resolve and uncomment
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

fun Project.getSourceSetsFrom(projectPath: String): SourceSetContainer {
    evaluationDependsOn(projectPath)
    return project(projectPath).sourceSets
}

val Project.sourceSets: SourceSetContainer
    get() = javaPluginConvention().sourceSets

val Project.mainSourceSet: SourceSet
    get() = sourceSets.getByName("main")

val Project.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")
