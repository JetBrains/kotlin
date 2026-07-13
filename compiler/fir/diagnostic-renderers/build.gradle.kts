plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("require-explicit-types")
}

dependencies {
    api(project(":compiler:fir:tree"))
    api(project(":compiler:fir:cones"))
    api(project(":compiler:frontend.common"))
    api(project(":core:compiler.common"))
    api(project(":core:metadata"))
    api(project(":kotlin-stdlib"))
    implementation(project(":compiler:fir:providers"))
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":core:util.runtime"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { none() }
}
