plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(libs.junit4)
    implementation(project(":compiler:tests-mutes"))
    api("com.nordstrom.tools:junit-foundation:17.2.4")
    // override beanutils dependency due to CVE-2025-48734
    api(libs.apache.commons.beanutils)

    constraints {
        api(libs.apache.commons.lang)
    }
}

sourceSets {
    "main" { projectDefault() }
}
