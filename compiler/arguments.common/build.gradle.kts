plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":core:language.version-settings"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
