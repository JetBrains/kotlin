plugins {
    `java-library`
}

val intellijVersion = rootProject.extra["versions.intellijSdk"]

//https://jetbrains.team/p/ij/repositories/intellij/files/aea53cfc5d27b15246ab7b7a0b5679d0d8cf1875/community/build/groovy/org/jetbrains/intellij/build/IntelliJCoreArtifactsBuilder.groovy?tab=source&line=18
//CORE_MODULES = [
//    "intellij.platform.util.rt",
//    "intellij.platform.util.classLoader",
//    "intellij.platform.util.text.matching",
//    "intellij.platform.util.base",
//    "intellij.platform.util.xmlDom",
//    "intellij.platform.util",
//    "intellij.platform.core",
//    "intellij.platform.core.impl",
//    "intellij.platform.extensions",
//    "intellij.java.psi",
//    "intellij.java.psi.impl",
//]

dependencies {
    api(project(":dependencies:intellij-java-psi-api"))
    api(project(":dependencies:intellij-core-implementation"))
}