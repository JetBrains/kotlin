plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:backend"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:resolution"))
    implementation(project(":compiler:serialization"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

optInToK1Deprecation()
