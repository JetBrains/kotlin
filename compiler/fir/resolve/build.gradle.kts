plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:resolution.common"))
    api(project(":compiler:fir:cones"))
    api(project(":compiler:fir:tree"))
    api(kotlinxCollectionsImmutable())
    implementation(project(":core:util.runtime"))

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

tasks {
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            // Workaround for KT-48445.
            languageVersion = "1.5"
            apiVersion = "1.5"
        }
    }
}
