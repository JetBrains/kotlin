plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":core:descriptors"))
    // This dependency is required only because of PackagePartClassUtils in TestProcessor.
    // TODO (KT-84117)
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:frontend.common-psi")) // required for error reporting
    api(project(":native:native.config"))
    api(project(":native:base"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
