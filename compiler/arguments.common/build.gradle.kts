plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":compiler:util"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
