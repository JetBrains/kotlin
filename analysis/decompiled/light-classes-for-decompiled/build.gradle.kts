plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":analysis:decompiled:decompiler-to-psi"))
    api(project(":analysis:light-classes-base"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
