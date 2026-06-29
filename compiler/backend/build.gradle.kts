plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-annotations-jvm"))
    api(project(":compiler:util"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    api(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:ir.tree"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.intellij.asm)
    compileOnly(libs.guava)
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
