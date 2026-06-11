plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

dependencies {
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":kotlin-util-klib-metadata"))
    compileOnly(intellijCore())
}

optInToK1Deprecation()
