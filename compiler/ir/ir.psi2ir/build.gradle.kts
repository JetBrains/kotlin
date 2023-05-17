plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:ir.tree"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks {
    @Suppress("UNUSED_VARIABLE")
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI"
        }
    }
}

