plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:fir-jvm"))

    /*
     * We can't remove this dependency until we use
     *   diagnostics framework from FE 1.0
     */
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:psi"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { none() }
}

generatedDiagnosticContainersAndCheckerComponents()
