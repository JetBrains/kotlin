import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-annotations-jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    api(project(":compiler:util"))
    api(project(":compiler:config"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:resolution"))
    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:psi:psi-utils"))
    api(project(":compiler:psi:psi-frontend-utils"))
    api(project(":compiler:psi:parser"))
    api(project(":compiler:frontend.common"))
    api(project(":compiler:frontend.common-psi"))
    api(libs.vavr)
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

optInToK1Deprecation()

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}
