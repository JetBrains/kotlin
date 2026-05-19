plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:frontend"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    api(project(":core:metadata.jvm"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":core:deserialization.common.jvm"))
    implementation(project(":compiler:frontend.java"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
