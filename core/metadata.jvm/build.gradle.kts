plugins {
    id("root-config")
    kotlin("jvm")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:metadata"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
