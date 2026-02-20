plugins {
    id("root-config")
    kotlin("jvm")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    implementation(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
}
