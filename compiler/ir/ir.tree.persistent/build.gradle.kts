plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:ir.tree"))
    implementation(project(":compiler:ir.serialization.common"))
}

sourceSets {
    "main" {
        projectDefault()
        this.java.srcDir("gen")
    }
    "test" {}
}
