plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":core:compiler.common"))
    implementation(project(":core:compiler.common.web"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
