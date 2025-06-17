plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

val runtimeOnly by configurations
val compileOnly by configurations
runtimeOnly.extendsFrom(compileOnly)

dependencies {
    implementation(project(":compiler:cli:cli-arguments-generator"))
    implementation(project(":generators"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:arguments"))
    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:build-tools:util-kotlinpoet"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
    compileOnly(intellijCore())
    runtimeOnly(intellijJDom())
}

application {
    mainClass.set("org.jetbrains.kotlin.buildtools.options.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
