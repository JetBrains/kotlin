plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:ir.tree"))
}

sourceSets {
    "main" {
        projectDefault()
        this.java.srcDir("gen")
    }
    "test" {}
}
