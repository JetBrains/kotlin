plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:resolution.common"))
    api(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
