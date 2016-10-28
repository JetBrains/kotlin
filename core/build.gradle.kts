
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

fun KotlinDependencyHandler.protobufLite() = project(":custom-dependencies:protobuf-lite", configuration = "default").apply { isTransitive = false }
fun protobufLiteTask() = ":custom-dependencies:protobuf-lite:prepare"

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
        listOf("core/descriptor.loader.java/src",
               "core/descriptors/src",
               "core/descriptors.runtime/src",
               "core/deserialization/src",
               "core/util.runtime/src")
        .map { File(rootDir, it) }
        .let { java.setSrcDirs(it) }
//        println(compileClasspath.joinToString("\n    ", prefix = "classpath =\n    ") { it.canonicalFile.relativeTo(rootDir).path })
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask())
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask())
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package")
}

//tasks.withType<Jar> {
//    enabled = false
//}
