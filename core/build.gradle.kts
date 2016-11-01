
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
    }
}

apply { plugin("kotlin") }

fun commonDep(coord: String): String {
    val parts = coord.split(':')
    return when (parts.size) {
        1 -> "$coord:$coord:${rootProject.extra["versions.$coord"]}"
        2 -> "${parts[0]}:${parts[1]}:${rootProject.extra["versions.${parts[1]}"]}"
        3 -> coord
        else -> throw IllegalArgumentException("Illegal maven coordinates: $coord")
    }
}

fun commonDep(group: String, artifact: String): String = "$group:$artifact:${rootProject.extra["versions.$artifact"]}"

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

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
    mavenCentral()
}

dependencies {
    compile(project(":core.builtins"))
    compile(project(":libraries:stdlib"))
    compile(protobufLite())
    compile(commonDep("javax.inject"))
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        java.setSrcDirs(
                listOf("core/descriptor.loader.java/src",
                       "core/descriptors/src",
                       "core/descriptors.runtime/src",
                       "core/deserialization/src",
                       "core/util.runtime/src")
                .map { File(rootDir, it) })
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-core")
}

fixKotlinTaskDependencies()

task("sourcesets") {
    doLast {
        the<JavaPluginConvention>().sourceSets.all {
            println("--> ${it.name}: ${it.java.srcDirs.joinToString()}")
        }
    }
}
