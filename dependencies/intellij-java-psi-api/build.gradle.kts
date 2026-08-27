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
    "com.jetbrains.intellij.platform:util-base:$intellijVersion",
    "com.jetbrains.intellij.platform:util:$intellijVersion",
    "com.jetbrains.intellij.platform:core:$intellijVersion",
    "com.jetbrains.intellij.platform:core-impl:$intellijVersion",
    "com.jetbrains.intellij.platform:extensions:$intellijVersion",
    "com.jetbrains.intellij.java:java-frontback-psi:$intellijVersion",
    "com.jetbrains.intellij.java:java-psi:$intellijVersion",
)

dependencies {
    intellijArtifacts.forEach {
        compileOnly(it) { isTransitive = false }
        embedded(it) { isTransitive = false }
    }

    compileOnly(kotlinStdlib())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.org.jetbrains.annotations)
    compileOnly(libs.kotlinx.coroutines.core)
}

sourceSets {
    "main" {
        projectDefault()
    }
}
