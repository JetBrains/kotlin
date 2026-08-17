plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

// Shaded into `kotlin-reflect`, whose `dexMethodCount` dexes the jar with a D8 that cannot read the
// `MethodParameters` attribute a modern `javac` emits for bridge methods even under `--release 8`.
project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    compileOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
