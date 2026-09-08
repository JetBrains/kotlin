plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":kotlin-util-klib-metadata"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
