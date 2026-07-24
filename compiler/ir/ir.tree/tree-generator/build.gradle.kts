plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
}

configurations.runtimeOnly.get().extendsFrom(configurations.compileOnly.get())

dependencies {
    implementation(project(":generators:tree-generator-common"))

    compileOnly(intellijCore())

    runtimeOnly(intellijJDom())
}

application {
    mainClass.set("org.jetbrains.kotlin.ir.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
