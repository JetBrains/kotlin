plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":core:metadata"))
    api(project(":core:deserialization.common"))
    api(project(":core:util.runtime"))
    implementation(project(":core:descriptors"))
    api(commonDependency("javax.inject"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
