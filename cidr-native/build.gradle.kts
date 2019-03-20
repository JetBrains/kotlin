import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val cidrUnscrambledJarDir: File by rootProject.extra

dependencies {
    addIdeaNativeModuleDeps()
    compileOnly(fileTree(cidrUnscrambledJarDir) { include("**/*.jar") })
}

defaultSourceSets()
