plugins {
    kotlin("jvm")
    id("jps-compatible")
//    id("gradle-plugin-compiler-dependency-configuration")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    implementation(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
}
