plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:config"))
    implementation(project(":compiler:container"))
    api(project(":core:language.syntax"))

    // Temporary dependency that's needed for `syntaxId()` that maps `IElementType` with `SyntaxId`
    // Once `KtPsiSourceElement` and `KtLightSourceElement` leaves this module, the dependency must be dropped.
    // TODO: KT-91461
    implementation(project(":compiler:psi:psi-api"))

    api(project(":core:deserialization.common"))
    api(project(":compiler:plugin-api"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}


generatedConfigurationKeys("FrontendConfigurationKeys")
