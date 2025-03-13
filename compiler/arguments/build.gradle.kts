plugins {
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization")
    id("jps-compatible")
    application
}

dependencies {
    api(kotlinStdlib())
    api(libs.kotlinx.serialization.json)
    api(project(":compiler:arguments.common"))
}

application {
    mainClass = "CompilerArgumentsKt"
}
