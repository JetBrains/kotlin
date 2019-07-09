
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":core:descriptors"))
    compile(project(":core:deserialization"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
