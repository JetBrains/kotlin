import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

repositories {
    maven("https://dl.bintray.com/jetbrains/markdown")
}

dependencies {
    compile(ultimateProjectDep(":cidr-native"))
    compileOnly(ultimateProjectDep(":prepare-deps:platform-deps", configuration = "clionUnscrambledJar"))

    if (!isStandaloneBuild) {
        compileOnly("org.jetbrains:markdown:$markdownVersion")
    }
}

defaultSourceSets()
