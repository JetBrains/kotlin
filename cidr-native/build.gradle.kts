import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val clionUnscrambledJarDir: File by rootProject.extra

dependencies {
    addIdeaNativeModuleDeps()
    compileOnly(fileTree(clionUnscrambledJarDir) { include("**/*.jar") })
}

defaultSourceSets()
