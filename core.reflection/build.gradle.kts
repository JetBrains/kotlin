
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.org.objectweb.asm.*
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath(files("$rootDir/ideaSDK/lib/asm-all.jar"))
    }
}

apply {
    plugin("kotlin")
    plugin("com.github.johnrengelman.shadow")
}

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
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
    compile(project(":core.builtins"))
    compile(project(":core"))
    compile(project(":libraries:stdlib"))
    compile(protobufLite())
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-reflection")
}

fixKotlinTaskDependencies()

