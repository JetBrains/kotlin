plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
}

dependencies {
    api(project(":core:compiler.common.js"))
    api(project(":js:js.ast"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.web.common"))

    // FE checks for modules use ModuleKind
    // This dependency can be removed when we stop supporting PLAIN and UMD module systems
    implementation(project(":js:js.serializer"))

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
