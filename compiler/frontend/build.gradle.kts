import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
//    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":kotlin-annotations-jvm"))
    api(project(":core:descriptors"))
    api(project(":core:deserialization"))
    api(project(":compiler:util"))
    api(project(":compiler:config"))
    api(project(":compiler:container"))
    api(project(":compiler:resolution"))
    api(project(":compiler:psi"))
    api(project(":compiler:frontend.common"))
    api(project(":compiler:frontend.common-psi"))
    api(project(":kotlin-script-runtime"))
    api(commonDependency("io.javaslang","javaslang"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}
