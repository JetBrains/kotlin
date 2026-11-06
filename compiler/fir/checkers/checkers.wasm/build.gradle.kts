plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
    id("require-explicit-types")
}

dependencies {
    api(project(":compiler:fir:checkers:checkers.web.common"))
    implementation(project(":core:compiler.common.wasm"))
    implementation(project(":core:compiler.common.js"))
    implementation(project(":core:compiler.common.web"))
    implementation(project(":core:error.handling"))

    // Needed for JS identifier utils
    implementation(project(":js:js.ast"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { none() }
}

generatedDiagnosticContainersAndCheckerComponents()
