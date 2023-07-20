plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    implementation(project(":compiler:tests-mutes"))
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.module.kotlin)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val mutesPackageName = "org.jetbrains.kotlin.test.mutes"

application {
    mainClass.set("$mutesPackageName.MutedTestsSyncKt")
    applicationDefaultJvmArgs = rootProject.properties.filterKeys { it.startsWith(mutesPackageName) }.map { (k, v) -> "-D$k=$v" }
}
