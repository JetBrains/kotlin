plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.jklib"))
    implementation(project(":compiler:backend.jvm"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":compiler:cli-jvm"))
    implementation(project(":native:native.config"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
}

optInToUnsafeDuringIrConstructionAPI()