plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:serialization"))
    compile(project(":kotlin-util-klib"))
    compile(project(":kotlin-util-klib-metadata"))
    compile(project(":compiler:util"))
    implementation(project(":compiler:psi"))
    compileOnly(project(":kotlin-reflect-api"))

    // TODO: move usages of JvmAnnotationNames and hasEnhancedNullability to ir.serialization.jvm and remove this dependency.
    implementation(project(":core:compiler.common.jvm"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks {
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            freeCompilerArgs += "-Xopt-in=org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI"
            freeCompilerArgs += "-Xinline-classes"
        }
    }
}
