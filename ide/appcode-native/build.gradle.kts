import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extra
val enableTasksIfAtLeast: (Project, String, Int) -> Unit by ultimateTools
val enableTasksIfOsIsNot: (Project, List<String>) -> Unit by ultimateTools

val appcodeVersion: String by rootProject.extra
val appcodeUnscrambledJarDir: File by rootProject.extra

dependencies {
    compile(project(":kotlin-ultimate:ide:cidr-native"))
    compileOnly(fileTree(appcodeUnscrambledJarDir) { include("**/*.jar") })
}

defaultSourceSets()

enableTasksIfAtLeast(project, appcodeVersion, 191)
enableTasksIfOsIsNot(project, listOf("Windows"))
