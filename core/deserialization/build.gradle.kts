plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:metadata"))
    api(project(":core:deserialization.common"))
    api(project(":core:util.runtime"))
    api(project(":core:descriptors"))
    api(commonDependency("javax.inject"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
