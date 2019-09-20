plugins {
    kotlin("jvm")
}

val clionUnscrambledJarDir: File by rootProject.extra
val clionVersion: String by rootProject.extra

val isStandaloneBuild: Boolean = rootProject.findProject(":idea") == null
val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

if (!isStandaloneBuild) {
    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/markdown")
        }
        maven("https://jetbrains.bintray.com/markdown")
    }
}
repositories {
    maven("https://maven.google.com")
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
}

dependencies {
    compile(project(":kotlin-ultimate:ide:cidr-native"))
    compile("com.jetbrains.intellij.cidr:cidr-cocoa-common:$clionVersion") { isTransitive = false }
    compile("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$clionVersion") { isTransitive = false }
    compileOnly(fileTree(clionUnscrambledJarDir) { include("**/*.jar") })
    compile("com.android.tools.ddms:ddmlib:26.0.0")

    if (!isStandaloneBuild) {
        compileOnly("org.jetbrains:markdown:${rootProject.extra["versions.markdown"]}")
    }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
