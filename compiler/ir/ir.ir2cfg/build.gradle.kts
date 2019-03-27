import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:ir.tree"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    freeCompilerArgs = freeCompilerArgs + listOf(
        "-Xuse-experimental=kotlin.Experimental",
        "-Xuse-experimental=org.jetbrains.kotlin.ir.DescriptorInIrDeclaration")
}
