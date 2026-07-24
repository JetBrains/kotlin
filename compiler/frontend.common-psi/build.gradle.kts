plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:frontend.common"))
    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:psi:psi-impl"))
    api(project(":compiler:psi:psi-frontend-utils"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
