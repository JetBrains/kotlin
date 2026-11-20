plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
    id("require-explicit-types")
}

dependencies {
    api(project(":compiler:fir:checkers:checkers.common"))
    implementation(project(":core:compiler.common.web"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":core:error.handling"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { none() }
}

generatedDiagnosticContainersAndCheckerComponents()
