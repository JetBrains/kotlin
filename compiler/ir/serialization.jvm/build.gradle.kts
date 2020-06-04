plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.serialization.common"))
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":core:metadata.jvm"))
    compile(project(":compiler:frontend.java"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

tasks {
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            freeCompilerArgs += "-Xopt-in=org.jetbrains.kotlin.ir.DescriptorBasedIr"
        }
    }
}