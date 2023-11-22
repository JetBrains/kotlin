plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:compiler.common"))
    implementation(project(":core:compiler.common.web"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
