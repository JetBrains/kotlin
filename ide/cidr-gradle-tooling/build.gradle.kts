import org.gradle.jvm.tasks.Jar

description = "Kotlin/Native Gradle Tooling support"

plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addKotlinGradleToolingDeps: (Project) -> Unit by ultimateTools

dependencies {
    addKotlinGradleToolingDeps(project)
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

(tasks.findByName("jar") as Jar).apply {
    archiveFileName.set("kotlin-cidr-gradle-tooling.jar")
}
