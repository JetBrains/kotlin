plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
}

// See ":dependencies:intellij-core" for the complete list of modules included in "intellij-core"

val intellijVersion = kotlinBuildProperties.versionsProperty("intellijSdk").get()

dependencies {
    api("com.jetbrains.intellij.platform:util-base:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:core:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:core-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:extensions:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-frontback-psi:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-psi:$intellijVersion") { isTransitive = false }
}
