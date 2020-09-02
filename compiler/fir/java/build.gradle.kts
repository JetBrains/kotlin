
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common.jvm"))
    api(project(":core:metadata.jvm"))
    api(project(":compiler:resolution.common.jvm"))
    api(project(":compiler:frontend.common"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:jvm"))
    api(project(":compiler:fir:fir-deserialization"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
