plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:serialization"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-util-klib-metadata"))
    api(project(":compiler:util"))
    implementation(project(":compiler:psi"))
    compileOnly(project(":kotlin-reflect"))

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks {
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI"
        }
    }
}
