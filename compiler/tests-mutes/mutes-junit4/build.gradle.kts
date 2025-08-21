plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(libs.junit4)
    implementation(project(":compiler:tests-mutes"))
    api("com.nordstrom.tools:junit-foundation:17.2.4")
    // override beanutils dependency due to CVE-2025-48734
    api(libs.apache.commons.beanutils)
}

sourceSets {
    "main" { projectDefault() }
}
