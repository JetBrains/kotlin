plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
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
