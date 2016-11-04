
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
    compile(project(":core.builtins"))
    buildVersion()
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

//configure<JavaPluginConvention> {
//    sourceSets.getByName("main").apply {
//        java.setSrcDirs(listOf(File(projectDir, "src")))
//    }
//    sourceSets.getByName("test").apply {
//        java.setSrcDirs(emptyList<File>())
//    }
//}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-script-runtime")
}

tasks.withType<Jar> {
    setupRuntimeJar("Kotlin Script Runtime")
    archiveName = "kotlin-script-runtime.jar"
}

fixKotlinTaskDependencies()
