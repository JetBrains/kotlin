import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    java
    id("gradle-plugin-compiler-dependency-configuration")
}

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
