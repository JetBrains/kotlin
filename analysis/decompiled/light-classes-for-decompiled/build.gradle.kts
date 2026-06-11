plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:light-classes-base"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
