plugins {
    kotlin("jvm")
    id("jps-compatible")
}

configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
