plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
}
