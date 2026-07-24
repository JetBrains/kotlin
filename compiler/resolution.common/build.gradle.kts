plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:config"))
    api(project(":compiler:util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
