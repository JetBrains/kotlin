plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(libs.junit.jupiter.api)
    implementation(project(":compiler:tests-mutes"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
