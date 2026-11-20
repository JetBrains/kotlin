plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
    id("require-explicit-types")
}

dependencies {
    api(project(":core:compiler.common.js"))
    api(project(":js:js.ast"))
    api(project(":compiler:fir:checkers:checkers.web.common"))

    implementation(project(":js:js.config"))
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
