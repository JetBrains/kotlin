
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compileOnly(intellijDep()) { includeJars("idea", "openapi", "util", "jdom") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

