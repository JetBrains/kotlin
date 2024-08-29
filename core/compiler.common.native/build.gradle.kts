plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    implementation(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
}
