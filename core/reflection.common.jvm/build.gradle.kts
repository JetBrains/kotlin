plugins {
    kotlin("jvm")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:compiler.common.jvm"))
}

sourceSets {
    "main" { projectDefault() }
}
