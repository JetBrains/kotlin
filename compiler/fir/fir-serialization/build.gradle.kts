plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("require-explicit-types")
}

dependencies {
    implementation(project(":core:compiler.common"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:fir:fir-deserialization"))
    implementation(project(":compiler:serialization"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":kotlin-util-klib-metadata"))

    api(project(":compiler:fir:cones"))
    api(project(":compiler:fir:tree"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:resolve"))

    compileOnly(intellijCore())
}

kotlin {
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
            "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
            "org.jetbrains.kotlin.types.model.K2Only",
        )
    )
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

