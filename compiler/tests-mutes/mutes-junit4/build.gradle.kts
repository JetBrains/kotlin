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

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "ch.qos.logback" && requested.name == "logback-core") {
            useVersion("1.5.19")
            because("CVE-2025-11226")
        }
    }
}

sourceSets {
    "main" { projectDefault() }
}
