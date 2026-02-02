plugins {
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
    // This dependency is required only because of PackagePartClassUtils in TestProcessor.
    // TODO (KT-84117)
    implementation(project(":compiler:frontend.common.jvm"))
    api(project(":native:native.config"))
    api(project(":native:base"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
