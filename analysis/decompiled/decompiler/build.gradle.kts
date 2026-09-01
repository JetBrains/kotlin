plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:psi:psi-impl"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))

    api(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
