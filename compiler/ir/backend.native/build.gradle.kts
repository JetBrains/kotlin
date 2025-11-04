plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijCore())
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":compiler:ir.serialization.native"))
    api(project(":native:base"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

