plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors.jvm"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:fir:java"))
    api(project(":compiler:fir:raw-fir:psi2fir"))
    api(project(":compiler:fir:raw-fir:light-tree2fir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:checkers:checkers.native"))
    api(project(":js:js.frontend"))

    // TODO: do not use GeneratorExtensions in `FirAnalyzerFacade.convertToIr`, and make this an 'implementation' dependency.
    api(project(":compiler:ir.psi2ir"))

    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":compiler:backend.jvm"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:ir.serialization.jvm"))
    implementation(project(":compiler:ir.serialization.js"))
    implementation(project(":compiler:ir.tree"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
