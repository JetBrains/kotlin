import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    id("jps-compatible")
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

val kotlinVersion: String by rootProject.extra

dependencies {
    compileOnly("org.jetbrains:annotations:13.0")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}

tasks.named<ProcessResources>("processResources") {
    val kotlinVersionLocal = kotlinVersion
    inputs.property("compilerVersion", kotlinVersionLocal)
    filesMatching("META-INF/compiler.version") {
        filter<ReplaceTokens>("tokens" to mapOf("snapshot" to kotlinVersionLocal))
    }
}