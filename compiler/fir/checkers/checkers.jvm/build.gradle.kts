plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
    id("require-explicit-types")
}

dependencies {
    api(project(":compiler:fir:checkers:checkers.common"))
    api(project(":compiler:fir:fir-jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":compiler:frontend.common.jvm",))
    implementation(project(":compiler:config.jvm"))
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
