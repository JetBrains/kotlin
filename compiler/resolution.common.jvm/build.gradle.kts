plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:resolution.common"))
    api(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
