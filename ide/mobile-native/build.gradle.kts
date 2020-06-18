import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

val cidrVersion: String by rootProject.extra

val ultimateTools: Map<String, Any> by rootProject.extensions
val proprietaryRepositories: Project.() -> Unit by ultimateTools
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools

proprietaryRepositories()

repositories {
    maven("https://maven.google.com")
}

dependencies {
    addIdeaNativeModuleDeps(project)
    compile(project(":kotlin-ultimate:ide:common-cidr-native"))
    compile(project(":kotlin-ultimate:ide:common-cidr-swift-native"))
    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa-common:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-xctest:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.swift:swift:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.swift:swift-language:$cidrVersion") { isTransitive = false }
    api(project(":kotlin-ultimate:ide:common-cidr-mobile")) { isTransitive = false }
    compileOnly("com.jetbrains.intellij.android:android-kotlin-extensions-common:$cidrVersion") { isTransitive = false }
    compile("com.android.tools.ddms:ddmlib:26.0.0") {
        exclude("com.google.guava", "guava")
    }
    compile(project(":kotlin-ultimate:libraries:tools:apple-gradle-plugin-api"))
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xno-optimized-callable-references"
}