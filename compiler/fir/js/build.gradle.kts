
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common.js"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:tree"))

    compileOnly(intellijCore())
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
