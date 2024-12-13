plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common"))

    implementation(project(":compiler:util"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    api(project(":generators"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
