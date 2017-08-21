@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
//import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

inline fun Project.sourceSets(crossinline body: SourceSetsBuilder.() -> Unit) =
        SourceSetsBuilder(this).body()

class SourceSetsBuilder(val project: Project) {

    inline operator fun String.invoke(crossinline body: SourceSet.() -> Unit) {
        val sourceSetName = this
        project.configure<JavaPluginConvention>
        {
            sourceSets.matching { it.name == sourceSetName }.forEach {
                none()
                it.body()
            }
        }
    }
}

fun SourceSet.none() {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

fun SourceSet.projectDefault() {
    when (name) {
        "main" -> {
            java.srcDirs("src")
            resources.srcDir("resources").apply { include("**") }
            resources.srcDir("src").apply { include("META-INF/**", "**/*.properties") }
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
