import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val appcodeUnscrambledJarDir: File by rootProject.extra

dependencies {
    compile(ultimateProjectDep(":cidr-native"))
    compileOnly(fileTree(appcodeUnscrambledJarDir) { include("**/*.jar") })
}

defaultSourceSets()
