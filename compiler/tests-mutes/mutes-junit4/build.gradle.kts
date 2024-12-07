plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(libs.junit4)
    implementation(project(":compiler:tests-mutes"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
