plugins {
    `java-library`
}

// See ":dependencies:intellij-core" for the complete list of modules included in "intellij-core"

val intellijVersion = rootProject.extra["versions.intellijSdk"]

dependencies {
    api("com.jetbrains.intellij.platform:util-rt:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-class-loader:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:core-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-frontback-psi-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-psi-impl:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:diagnostic:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:diagnostic-telemetry:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:util-progress:$intellijVersion") { isTransitive = false }
    runtimeOnly("com.jetbrains.intellij.platform:util-coroutines:$intellijVersion") { isTransitive = false }
    runtimeOnly(libs.opentelemetry.api) { isTransitive = false }
}