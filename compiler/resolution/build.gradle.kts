
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":core:descriptors"))
    compileOnly(intellijDep()) { includeJars("trove4j") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
