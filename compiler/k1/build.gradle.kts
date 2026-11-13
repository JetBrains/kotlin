import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-annotations-jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:config"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-utils"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":compiler:psi:parser"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(libs.vavr)
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

optInToK1Deprecation()
optInToObsoleteDescriptorBasedAPI()

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}
