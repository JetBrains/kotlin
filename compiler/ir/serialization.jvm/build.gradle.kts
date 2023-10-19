plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":core:descriptors.jvm"))
    api(project(":core:metadata.jvm"))
    implementation(project(":core:deserialization.common.jvm"))
    api(project(":compiler:frontend.java"))
}

optInToIrSymbolInternals()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
