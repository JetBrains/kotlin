import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    id("jps-compatible")
//    id("gradle-plugin-compiler-dependency-configuration")
}

// This module does not apply Kotlin plugin, so we are setting toolchain via
// java extension
if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
    configureJavaOnlyToolchain(JdkMajorVersion.JDK_1_8)
}

val kotlinVersion: String by rootProject.extra

dependencies {
    compileOnly("org.jetbrains:annotations:13.0")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.named<ProcessResources>("processResources") {
    val kotlinVersionLocal = kotlinVersion
    inputs.property("compilerVersion", kotlinVersionLocal)
    filesMatching("META-INF/compiler.version") {
        filter<ReplaceTokens>("tokens" to mapOf("snapshot" to kotlinVersionLocal))
    }
}
