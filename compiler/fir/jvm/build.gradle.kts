plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":compiler:resolution.common.jvm"))
    implementation(project(":compiler:fir:cones"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:frontend.java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
