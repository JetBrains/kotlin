plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:psi"))
    implementation(intellijCore())
}

kotlin {
    explicitApi()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.ExperimentalStdlibApi"
}

sourceSets {
    "main" { projectDefault() }
}
