plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_6)

dependencies {
    api(protobufLite())
    api(kotlinStdlib())
    kotlinCompilerClasspath(project(":libraries:tools:stdlib-compiler-classpath"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
