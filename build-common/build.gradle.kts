
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

fun Jar.setupRuntimeJar(implementationTitle: String): Unit {
    dependsOn(":prepare:build.version:prepare")
    manifest.attributes.apply {
        put("Built-By", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Vendor", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Title", implementationTitle)
        put("Implementation-Version", rootProject.extra["build.number"])
    }
    from(configurations.getByName("build-version").files) {
        into("META-INF/")
    }
}

fun DependencyHandler.buildVersion(): Dependency {
    val cfg = configurations.create("build-version")
    return add(cfg.name, project(":prepare:build.version", configuration = "default"))
}

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
    compile(project(":prepare:runtime", configuration = "default"))
    compile(project(":prepare:reflect", configuration = "default"))
    compile(project(":core.script.runtime"))
    compile(project(":compiler"))
    compile(files("$rootDir/ideaSDK/lib/util.jar"))
    buildVersion()
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        java.setSrcDirs(listOf(File(projectDir, "src")))
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-build-common")
}

tasks.withType<Jar> {
    setupRuntimeJar("Kotlin Build Common")
    archiveName = "kotlin-script-runtime.jar"
}

fixKotlinTaskDependencies()
