plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:resolution.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

