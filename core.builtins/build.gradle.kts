
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializer
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
    }
}

apply { plugin("kotlin") }

val protobufLiteProject = ":custom-dependencies:protobuf-lite"
fun KotlinDependencyHandler.protobufLite(): ProjectDependency =
        project(protobufLiteProject, configuration = "default").apply { isTransitive = false }
val protobufLiteTask = "$protobufLiteProject:prepare"

fun Project.fixKotlinTaskDependencies() {
    the<JavaPluginConvention>().sourceSets.all { sourceset ->
        val taskName = if (sourceset.name == "main") "classes" else (sourceset.name + "Classes")
        tasks.withType<Task> {
            if (name == taskName) {
                dependsOn("copy${sourceset.name.capitalize()}KotlinClasses")
            }
        }
    }
}

// TODO: common ^ 8< ----

val builtinsSrc = File(rootDir, "core/builtins/src")
val builtinsNative = File(rootDir, "core/builtins/native")
val builtinsSerialized = File(buildDir, "builtins")
val builtinsJar = File(buildDir, "builtins.jar")

dependencies {
    compile(protobufLite())
    compile(files(builtinsSerialized))
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        java.setSrcDirs(listOf(File(rootDir, "core/builtins/src"), File(rootDir, "core/runtime.jvm/src")))
        resources.setSrcDirs(listOf(builtinsSerialized))
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

val serialize = task("internal.serialize") {
    val outDir = builtinsSerialized
    val inDirs = arrayOf(builtinsSrc, builtinsNative)
    outputs.file(outDir)
    inputs.files(*inDirs)
    doLast {
        BuiltInsSerializer(dependOnOldBuiltIns = false)
                .serialize(outDir, inDirs.asList(), listOf()) { totalSize, totalFiles ->
                    println("Total bytes written: $totalSize to $totalFiles files")
                }
    }
}

//task("sourcesets") {
//    doLast {
//        the<JavaPluginConvention>().sourceSets.all {
//            println("--> ${it.name}: ${it.java.srcDirs.joinToString()}")
//        }
//    }
//}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
    dependsOn(serialize)
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
    dependsOn(serialize)
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-builtins")
}

fixKotlinTaskDependencies()
