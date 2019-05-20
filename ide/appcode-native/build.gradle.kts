plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val enableTasksIfAtLeast: (Project, String, Int) -> Unit by ultimateTools
val enableTasksIfOsIsNot: (Project, List<String>) -> Unit by ultimateTools

val appcodeVersion: String by rootProject.extra
val appcodeUnscrambledJarDir: File by rootProject.extra

dependencies {
    compile(project(":kotlin-ultimate:ide:cidr-native"))
    compileOnly(fileTree(appcodeUnscrambledJarDir) { include("**/*.jar") })
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

enableTasksIfAtLeast(project, appcodeVersion, 191)
enableTasksIfOsIsNot(project, listOf("Windows"))
