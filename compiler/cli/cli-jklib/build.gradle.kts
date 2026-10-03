plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:ir.serialization.jklib"))
    implementation(project(":compiler:ir.inline"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":compiler:cli-jvm"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
}

optInToK1Deprecation()

optInToUnsafeDuringIrConstructionAPI()
