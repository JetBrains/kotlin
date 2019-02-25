import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val clionUnscrambledJarDir: File by rootProject.extra

repositories {
    maven("https://dl.bintray.com/jetbrains/markdown")
}

dependencies {
    compile(ultimateProjectDep(":cidr-native"))
    compileOnly(fileTree(clionUnscrambledJarDir) { include("**/*.jar") })

    if (!isStandaloneBuild) {
        compileOnly("org.jetbrains:markdown:${rootProject.extra["versions.markdown"]}")
    }
}

defaultSourceSets()
