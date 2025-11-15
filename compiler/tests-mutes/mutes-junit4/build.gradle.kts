plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    implementation(libs.junit4)
    implementation(project(":compiler:tests-mutes"))
    implementation("com.nordstrom.tools:junit-foundation:17.2.4")
    // override beanutils dependency due to CVE-2025-48734
    implementation(libs.apache.commons.beanutils)

    constraints {
        implementation(libs.apache.commons.lang)
    }
}

sourceSets {
    "main" { projectDefault() }
}
