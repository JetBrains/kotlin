plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":core:language-model"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
