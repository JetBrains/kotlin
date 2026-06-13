plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-annotations-jvm"))
    api(project(":compiler:util"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":core:descriptors.jvm"))
    api(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:serialization"))
    implementation(project(":compiler:backend.common.jvm"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.intellij.asm)
    compileOnly(libs.guava)
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

optInToK1Deprecation()
