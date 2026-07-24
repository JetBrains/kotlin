plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:serialization.common"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":core:descriptors"))
    api(project(":compiler:backend.jvm"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.serialization.jvm"))
    implementation(project(":compiler:backend.jvm.lower"))
    implementation(project(":compiler:backend.jvm.codegen"))
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
