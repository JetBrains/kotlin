plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:providers"))
    implementation(project(":core:util.runtime"))

    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
