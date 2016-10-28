
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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

fun KotlinDependencyHandler.protobufLite() = project(":custom-dependencies:protobuf-lite", configuration = "default").apply { isTransitive = false }
fun protobufLiteTask() = ":custom-dependencies:protobuf-lite:prepare"

// TODO: common ^ 8< ----

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        java.setSrcDirs(listOf(
                File(rootDir, "core/reflection/src"),
                File(rootDir, "core/reflection.java/src")))
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

dependencies {
    compile(project(":core.builtins"))
    compile(project(":core"))
    compile(project(":libraries:stdlib"))
    compile(protobufLite())
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask())
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask())
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package")
}

