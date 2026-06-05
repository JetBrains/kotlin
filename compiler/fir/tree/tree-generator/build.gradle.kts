plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
}

configurations.runtimeOnly.get().extendsFrom(configurations.compileOnly.get())

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
