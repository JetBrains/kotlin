plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    // TODO: decouple from backend.common
    compile(project(":compiler:frontend"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:serialization"))
    compile(project(":kotlin-util-klib"))
    compile(project(":kotlin-util-klib-metadata"))

    compile(project(":compiler:util"))
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":compiler:ir.backend.common"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks {
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            freeCompilerArgs += "-Xopt-in=org.jetbrains.kotlin.ir.DescriptorBasedIr"
        }
    }
}

