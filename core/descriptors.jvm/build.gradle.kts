plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_6)

dependencies {
    api(project(":kotlin-annotations-jvm"))
    api(project(":core:descriptors"))
    api(project(":core:deserialization"))
    api(project(":core:compiler.common.jvm"))
    api(project(":core:deserialization.common.jvm"))
    api(project(":core:util.runtime"))
    api(commonDep("javax.inject"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
