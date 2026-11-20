plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("require-explicit-types")
}

kotlin {
    explicitApiWarning()
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:fir:semantics.api"))
    api(project(":compiler:fir:checkers:checkers.api"))
    implementation(project(":core:util.runtime"))

    // TODO(KT-90142): Needed to preserve backwards compatibility in FirExtensionRegistrar.
    // These dependencies allow keeping all of the 'unaryPlus` overloads without including those specific extensions in the plugin API.
    compileOnly(project(":compiler:fir:fir-serialization"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:fir2ir"))

    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
