plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.jvm"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    implementation(libs.intellij.asm)
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
