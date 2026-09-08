plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    java
}

dependencies {
    implementation(intellijRuntimeAnnotations())
}

jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_17_0
}

tasks.withType<JavaCompile> {
    // deprecating a package is a `@Deprecated annotation has no effect on packages` warning in Java,
    // but we need to deprecate it for a 3rd party plugin compatibility checker
    options.compilerArgs.remove("-Werror")
}


sourceSets {
    "main" { generatedDir() }
    "test" { none() }
}
