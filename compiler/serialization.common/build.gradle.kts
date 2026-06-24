plugins {
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
