plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:util"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:resolution"))
    implementation(project(":compiler:resolution.common"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.tree"))
    implementation(project(":compiler:ir.serialization.common"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()
optInToObsoleteDescriptorBasedAPI()
optInTo("org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi")

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

optInToK1Deprecation()
