plugins {
    kotlin("jvm")
    id("generated-sources")
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
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:util"))
    /*
     * We can't remove this dependency until we use
     *   diagnostics framework from FE 1.0
     */
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:fir:fir-native"))
    implementation(project(":compiler:fir:resolve"))

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
