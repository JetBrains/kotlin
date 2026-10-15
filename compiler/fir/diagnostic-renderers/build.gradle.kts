plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("require-explicit-types")
}

dependencies {
    api(project(":compiler:frontend.common"))
    api(project(":core:compiler.common"))
    api(project(":core:metadata"))
    api(project(":kotlin-stdlib"))
    implementation(project(":compiler:fir:semantics.api"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":core:util.runtime"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { none() }
}
