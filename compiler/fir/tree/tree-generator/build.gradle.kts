plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

val runtimeOnly by configurations
val compileOnly by configurations
runtimeOnly.extendsFrom(compileOnly)

dependencies {
    implementation(project(":generators"))
    implementation(project(":generators:tree-generator-common"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:fir:cones"))

    compileOnly(intellijCore())

    runtimeOnly(intellijJDom())
}

application {
    mainClass.set("org.jetbrains.kotlin.fir.tree.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
