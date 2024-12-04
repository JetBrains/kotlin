plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common"))

    implementation(project(":compiler:util"))
    api(project(":generators"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
