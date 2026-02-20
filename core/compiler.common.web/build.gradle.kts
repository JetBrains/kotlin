plugins {
    id("root-config")
    kotlin("jvm")
}

configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
