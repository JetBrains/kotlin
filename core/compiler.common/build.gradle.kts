plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":core:util.runtime"))
    api(project(":core:names"))
    api(project(":core:language.model"))
    api(project(":core:language.targets"))
    api(kotlinStdlib())
    api(project(":kotlin-annotations-jvm"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
