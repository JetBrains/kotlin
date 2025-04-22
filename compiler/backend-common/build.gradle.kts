
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors"))
    api(project(":compiler:util"))
    implementation(project(":compiler:frontend"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
