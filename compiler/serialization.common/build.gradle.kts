plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":core:metadata"))
    api(project(":core:names"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
