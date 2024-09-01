plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:metadata.jvm"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    implementation(project(":core:compiler.common.jvm"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    implementation(project(":core:descriptors.jvm"))
    api(project(":compiler:psi"))

}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
