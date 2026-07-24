plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":kotlin-annotations-jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    api(project(":core:compiler.common.jvm"))
    api(project(":core:deserialization.common.jvm"))
    api(project(":core:util.runtime"))
    api(commonDependency("javax.inject"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

optInToK1Deprecation()
