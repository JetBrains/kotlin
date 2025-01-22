plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(libs.junit4)
    implementation(project(":compiler:tests-mutes"))
    api("com.nordstrom.tools:junit-foundation:17.1.1")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
