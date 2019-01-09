import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

dependencies {
    addIdeaNativeModuleDeps()
    compileOnly(ultimateProjectDep(":prepare-deps:platform-deps", configuration = "clionUnscrambledJar"))
}

defaultSourceSets()
