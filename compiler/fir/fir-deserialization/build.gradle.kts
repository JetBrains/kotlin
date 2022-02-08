plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":core:metadata"))
    implementation(project(":core:metadata.jvm"))
    implementation(project(":core:deserialization.common"))
    implementation(project(":core:deserialization.common.jvm"))
    implementation(project(":core:compiler.common"))
    implementation(project(":core:compiler.common.jvm"))

    api(project(":compiler:fir:cones"))
    api(project(":compiler:fir:tree"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
