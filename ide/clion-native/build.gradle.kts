import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val clionUnscrambledJarDir: File by rootProject.extra

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

if (!isStandaloneBuild) {
    repositories {
        if (cacheRedirectorEnabled) { maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/markdown") }
        maven("https://jetbrains.bintray.com/markdown")
    }
}

dependencies {
    compile(ultimateProjectDep(":ide:cidr-native"))
    compileOnly(fileTree(clionUnscrambledJarDir) { include("**/*.jar") })

    if (!isStandaloneBuild) {
        compileOnly("org.jetbrains:markdown:${rootProject.extra["versions.markdown"]}")
    }
}

defaultSourceSets()
