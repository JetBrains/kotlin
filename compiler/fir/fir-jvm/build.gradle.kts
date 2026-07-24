
plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("require-explicit-types")
}

dependencies {
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":core:metadata.jvm"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:checkers"))
    implementation(project(":compiler:fir:fir-deserialization"))

    api(project(":core:deserialization.common.jvm"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
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
    "test" {}
}
