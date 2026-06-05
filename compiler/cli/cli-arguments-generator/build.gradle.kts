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
    implementation(project(":generators"))
    implementation(project(":compiler:config"))
    implementation(project(":compiler:arguments"))

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
