plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.jklib"))
    implementation(project(":compiler:backend.jvm"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
}

optInToUnsafeDuringIrConstructionAPI()