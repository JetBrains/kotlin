plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":core:compiler.common"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
