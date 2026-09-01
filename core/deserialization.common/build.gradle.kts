plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":core:metadata"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
