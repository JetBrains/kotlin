plugins {
    java
    id("jps-compatible")
}

dependencies {
    implementation(intellijRuntimeAnnotations())
}

project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)

tasks.withType<JavaCompile> {
    // deprecating a package is a `@Deprecated annotation has no effect on packages` warning in Java,
    // but we need to deprecate it for a 3rd party plugin compatibility checker
    options.compilerArgs.remove("-Werror")
    targetCompatibility = "1.8"
    sourceCompatibility = "1.8"
}


sourceSets {
    "main" { generatedDir() }
    "test" { none() }
}
