plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:tree"))
    api(project(":compiler:fir:fir-jvm"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:fir-deserialization"))
    implementation("org.eclipse.jdt:ecj:3.41.0")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}