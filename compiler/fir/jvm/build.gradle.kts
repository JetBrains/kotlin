plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {

    implementation(project(":compiler:fir:cones"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:resolve"))
}



sourceSets {
    "main" { projectDefault() }
    "test" {}
}