plugins {
    kotlin("jvm")
}

val clionUnscrambledJarDir: File by rootProject.extra

val isStandaloneBuild: Boolean by rootProject.extra
val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

val ultimateTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val proprietaryRepositories: Project.() -> Unit by ultimateTools

if (!isStandaloneBuild) {
    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/markdown")
        }
        maven("https://jetbrains.bintray.com/markdown")
    }
}

proprietaryRepositories(project)

addIdeaNativeModuleDeps(project)

dependencies {
    compileOnly(fileTree(clionUnscrambledJarDir) { include("**/*.jar") })

    if (!isStandaloneBuild) {
        compileOnly("org.jetbrains:markdown:${rootProject.extra["versions.markdown"]}")
    }
    api(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    api(project(":kotlin-ultimate:ide:common-cidr-native")) { isTransitive = false }
    compileOnly(project(":kotlin-ultimate:ide:cidr-gradle-tooling")) { isTransitive = false }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
