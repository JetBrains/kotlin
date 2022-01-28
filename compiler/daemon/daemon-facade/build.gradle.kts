description = "Kotlin Daemon facade to unify running compilation via daemon, in-process, or out-of-process"

plugins {
    kotlin("jvm")
}

dependencies {
    // todo think about dependency configurations
    implementation(project(":kotlin-daemon-client"))
    implementation(project(":daemon-common"))
    implementation(project(":compiler:cli"))
}