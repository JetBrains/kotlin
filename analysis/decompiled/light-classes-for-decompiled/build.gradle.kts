plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-providers"))
    api(project(":analysis:project-structure"))
    api(project(":analysis:decompiled:decompiler-to-psi"))
    api(project(":compiler:light-classes"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
