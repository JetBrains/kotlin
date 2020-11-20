plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors.jvm"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:fir:java"))
    api(project(":compiler:fir:raw-fir:psi2fir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:checkers"))

    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":compiler:backend.jvm"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:ir.serialization.jvm"))
    implementation(project(":compiler:ir.tree.impl"))

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
