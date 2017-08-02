
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath(ideaSdkDeps("asm-all"))
    }
}

apply {
    plugin("kotlin")
    plugin("com.github.johnrengelman.shadow")
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main")?.apply {
        val srcs = listOf(File(rootDir, "core/reflection.jvm/src"))
        java.setSrcDirs(srcs)
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

dependencies {
    val compile by configurations
    compile(project(":core:builtins"))
    compile(project(":core"))
    compile(protobufLite())
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
}


