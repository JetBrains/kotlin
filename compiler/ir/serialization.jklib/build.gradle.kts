plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":compiler:ir.serialization.jvm"))
}

sourceSets {
    "main" { projectDefault() }
}
