plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(libs.junit4)
    implementation(project(":compiler:tests-mutes"))
    api("com.nordstrom.tools:junit-foundation:17.2.2")
}

sourceSets {
    "main" { projectDefault() }
}
