import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    java
    id("gradle-plugin-compiler-dependency-configuration")
}

// This module does not apply Kotlin plugin, so we are setting toolchain via
// java extension
configureJavaOnlyToolchain(JdkMajorVersion.JDK_1_8)


dependencies {
    compileOnly("org.jetbrains:annotations:13.0")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.named<ProcessResources>("processResources") {
    val kotlinVersionLocal = kotlinBuildProperties.kotlinVersion.get()
    inputs.property("compilerVersion", kotlinVersionLocal)
    filesMatching("META-INF/compiler.version") {
        filter<ReplaceTokens>("tokens" to mapOf("snapshot" to kotlinVersionLocal))
    }
}
