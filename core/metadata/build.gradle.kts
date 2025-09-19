plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-published-compiler-dependency-configuration") // via kotlin-util-klib
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(protobufLite())
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
