plugins {
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:fir-jvm"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":compiler:frontend.common.jvm",))
    implementation(project(":compiler:config.jvm"))

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
