plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
}

val runtimeOnly = configurations.getByName("runtimeOnly")
val compileOnly = configurations.getByName("compileOnly")
runtimeOnly.extendsFrom(compileOnly)

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
