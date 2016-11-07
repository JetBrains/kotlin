
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.jvm.tasks.Jar
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

dependencies {
    compile(project(":core:util.runtime"))
    compile(fileTree(mapOf("dir" to "$rootDir/ideaSDK/core", "include" to "*.jar")))
    compile(files("$rootDir/ideaSDK/jps/jps-model.jar"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-compiler.util")
}

fixKotlinTaskDependencies()
