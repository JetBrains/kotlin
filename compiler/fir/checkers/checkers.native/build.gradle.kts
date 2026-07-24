plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
    id("require-explicit-types")
}

dependencies {
    api(project(":compiler:fir:checkers"))
    api(project(":native:base"))
    api(project(":compiler:fir:cones"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:tree"))
    api(project(":compiler:frontend.common"))
    api(project(":core:compiler.common"))
    api(project(":core:util.runtime"))
    api(project(":kotlin-stdlib"))
    implementation(project(":compiler:fir:diagnostic-renderers"))
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:frontend.common-psi"))

    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:fir:fir-native"))

    compileOnly(project(":core:compiler.common.native"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { none() }
}

generatedDiagnosticContainersAndCheckerComponents()
