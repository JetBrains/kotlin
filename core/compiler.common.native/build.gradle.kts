plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
}
