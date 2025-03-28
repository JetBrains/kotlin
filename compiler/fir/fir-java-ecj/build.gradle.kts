plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:tree"))
    api(project(":compiler:fir:jvm"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:fir-deserialization"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}