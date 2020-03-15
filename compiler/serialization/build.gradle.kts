plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:resolution"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
