
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":core:metadata.jvm"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":compiler:resolution.common.jvm"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:checkers"))
    implementation(project(":compiler:fir:fir-deserialization"))

    api(project(":core:deserialization.common.jvm"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
            "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
            "org.jetbrains.kotlin.types.model.K2Only",
        )
    }
}
