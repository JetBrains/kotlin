plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
