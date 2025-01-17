plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(libs.junit4)
    implementation(project(":compiler:tests-mutes"))
    implementation("com.nordstrom.tools:junit-foundation:17.0.3")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
