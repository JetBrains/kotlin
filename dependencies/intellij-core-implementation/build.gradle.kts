plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    kotlin("jvm")
    id("intellij-patched-fat-jar")
}

// See ":dependencies:intellij-core" for the complete list of modules included in "intellij-core"

val intellijVersion = kotlinBuildProperties.versionsProperty("intellijSdk").get()

val intellijArtifacts = listOf(
    "com.jetbrains.intellij.platform:util-base-multiplatform:$intellijVersion",
    "com.jetbrains.intellij.platform:util-class-loader:$intellijVersion",
    "com.jetbrains.intellij.platform:util-multiplatform:$intellijVersion",
    "com.jetbrains.intellij.platform:util-rt:$intellijVersion",
    "com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion",
    "com.jetbrains.intellij.java:java-frontback-psi-impl:$intellijVersion",
    "com.jetbrains.intellij.java:java-psi-impl:$intellijVersion",
    "com.jetbrains.intellij.java:java-syntax:$intellijVersion",
    "com.jetbrains.intellij.platform:eel:$intellijVersion",
    "com.jetbrains.intellij.platform:plugin-system-parser-impl:$intellijVersion",
    "com.jetbrains.intellij.platform:syntax:$intellijVersion",
    "com.jetbrains.intellij.platform:syntax-extensions:$intellijVersion",
    "com.jetbrains.intellij.platform:syntax-psi:$intellijVersion",
    "com.jetbrains.intellij.platform:diagnostic:$intellijVersion",
    "com.jetbrains.intellij.platform:diagnostic-telemetry:$intellijVersion",
    "com.jetbrains.intellij.platform:syntax-i18-n:$intellijVersion",
    "com.jetbrains.intellij.platform:syntax-util:$intellijVersion",
    "com.jetbrains.intellij.platform:util-progress:$intellijVersion",
    "com.jetbrains.intellij.platform:util-coroutines:$intellijVersion",
)

dependencies {
    intellijArtifacts.forEach {
        compileOnly(it) { isTransitive = false }
        embedded(it) { isTransitive = false }
    }

    embedded(libs.opentelemetry.api) { isTransitive = false }

    compileOnly(project(":dependencies:intellij-java-psi-api"))
    compileOnly(kotlinStdlib())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.org.jetbrains.annotations)
}

sourceSets {
    "main" {
        projectDefault()
    }
}
