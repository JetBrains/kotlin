plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:resolution"))
    compile(project(":core:deserialization"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
