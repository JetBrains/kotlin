plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":core:deserialization.common"))
    implementation(project(":core:deserialization.common.jvm"))
    implementation(project(":core:deserialization"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":kotlin-util-klib"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToK1Deprecation()
