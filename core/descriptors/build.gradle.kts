plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:compiler.common"))
    implementation(project(":core:util.runtime"))
    implementation(kotlinStdlib())
    implementation(project(":kotlin-annotations-jvm"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
