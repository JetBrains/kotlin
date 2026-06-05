plugins {
    kotlin("jvm")
    application
}

configurations.runtimeOnly.get().extendsFrom(configurations.compileOnly.get())

dependencies {
    implementation(project(":generators"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:arguments"))
    implementation(project(":compiler:plugin-api"))

    compileOnly(intellijCore())

    runtimeOnly(intellijJDom())
    runtimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
}

application {
    mainClass.set("org.jetbrains.kotlin.cli.arguments.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
