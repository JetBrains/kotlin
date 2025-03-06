plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:fir:checkers"))

    /*
     * We can't remove this dependency until we use
     *   diagnostics framework from FE 1.0
     */
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:psi"))
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
