plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":core:descriptors.jvm"))
    api(project(":core:metadata.jvm"))
    implementation(project(":core:deserialization.common.jvm"))
    api(project(":compiler:frontend.java"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
