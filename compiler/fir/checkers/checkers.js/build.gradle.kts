plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
    id("require-explicit-types")
}

dependencies {
    api(project(":core:compiler.common.js"))
    api(project(":js:js.ast"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.web.common"))

    implementation(project(":compiler:fir:diagnostic-renderers"))
    implementation(project(":js:js.config"))

    /*
     * We can't remove this dependency until we use
     *   diagnostics framework from FE 1.0
     */
    implementation(project(":compiler:frontend"))
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
