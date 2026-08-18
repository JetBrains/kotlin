plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":analysis:decompiled:decompiler-to-psi"))
    api(project(":analysis:light-classes-base"))
    implementation(project(":analysis:analysis-api"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
