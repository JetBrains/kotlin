import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:metadata.jvm"))
    api(project(":core:deserialization.common"))
    implementation(project(":core:compiler.common.jvm"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
