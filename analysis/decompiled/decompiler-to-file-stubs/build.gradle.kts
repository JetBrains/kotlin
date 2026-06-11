plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":core:deserialization.common"))
    implementation(project(":core:deserialization.common.jvm"))
    implementation(project(":core:deserialization"))
    implementation(project(":analysis:decompiled:decompiler-to-stubs"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToK1Deprecation()
