import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

dependencies {
    compile(ultimateProjectDep(":cidr-native"))
    compileOnly(ultimateProjectDep(":prepare-deps:platform-deps", configuration = "appcodeUnscrambledJar"))
}

defaultSourceSets()
