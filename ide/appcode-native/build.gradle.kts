import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val appcodeVersion: String by rootProject.extra
val appcodeUnscrambledJarDir: File by rootProject.extra

dependencies {
    compile(ultimateProjectDep(":cidr-native"))
    compileOnly(fileTree(appcodeUnscrambledJarDir) { include("**/*.jar") })
}

defaultSourceSets()

enableTasksIfAtLeast(appcodeVersion, 191)
