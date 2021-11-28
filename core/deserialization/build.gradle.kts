plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_6)

dependencies {
    api(project(":core:metadata"))
    api(project(":core:deserialization.common"))
    api(project(":core:util.runtime"))
    api(project(":core:descriptors"))
    api(commonDep("javax.inject"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
