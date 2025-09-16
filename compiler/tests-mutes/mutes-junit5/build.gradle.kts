plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(libs.junit.jupiter.api)
    implementation(project(":compiler:tests-mutes"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
