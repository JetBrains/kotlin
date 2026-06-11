plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":core:compiler.common"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:decompiled:decompiler-to-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":compiler:util"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
