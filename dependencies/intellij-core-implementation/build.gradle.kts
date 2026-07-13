plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
}

// See ":dependencies:intellij-core" for the complete list of modules included in "intellij-core"

val intellijVersion = kotlinBuildProperties.versionsProperty("intellijSdk").get()

dependencies {
    api("com.jetbrains.intellij.platform:util-base-multiplatform:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-class-loader:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-multiplatform:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-rt:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-frontback-psi-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-psi-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-syntax:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:eel:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:plugin-system-parser-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:syntax:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:syntax-extensions:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:syntax-psi:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:diagnostic:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:diagnostic-telemetry:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:syntax-i18-n:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:syntax-util:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:util-progress:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:util-coroutines:$intellijVersion") { isTransitive = false }
    runtimeOnly(libs.opentelemetry.api) { isTransitive = false }
}
