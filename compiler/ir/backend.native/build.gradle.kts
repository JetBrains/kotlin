plugins {
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    compileOnly(intellijCore())
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":compiler:frontend.common.jvm")) // For TestProcessor shitty utility func
    api(project(":native:base"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

generatedConfigurationKeys("KonanConfigKeys")
