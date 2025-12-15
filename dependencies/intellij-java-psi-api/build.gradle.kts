plugins {
    `java-library`
}

// See ":dependencies:intellij-core" for the complete list of modules included in "intellij-core"

val intellijVersion = rootProject.extra["versions.intellijSdk"]

dependencies {
    api("com.jetbrains.intellij.platform:util-base:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:core:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:extensions:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-frontback-psi:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-psi:$intellijVersion") { isTransitive = false }
}