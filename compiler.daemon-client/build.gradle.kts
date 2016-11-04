
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

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
    mavenCentral()
}

val nativePlatformUberjar = "$rootDir/dependencies/native-platform-uberjar.jar"

dependencies {
    compile(project(":compiler"))
    // TODO check whether splitting by platform could be more effective on the runtime
    compile(files(nativePlatformUberjar))
    buildVersion()
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        java.setSrcDirs(listOf(File(rootDir, "compiler/daemon/daemon-client/src")))
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-daemon-client")
}

tasks.withType<Jar> {
    setupRuntimeJar("Kotlin Daemon Client")
    from(zipTree(nativePlatformUberjar))
    archiveName = "kotlin-daemon-client.jar"
}

fixKotlinTaskDependencies()
