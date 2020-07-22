
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

val cidrVersion: String by rootProject.extra
val isStandaloneBuild: Boolean by rootProject.extra

val ultimateTools: Map<String, Any> by rootProject.extensions
val proprietaryRepositories: Project.() -> Unit by ultimateTools
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val ijProductBranch: (String) -> Int by ultimateTools

proprietaryRepositories()

repositories {
    ivy {
        url = uri("https://buildserver.labs.intellij.net/guestAuth/repository/download")
        patternLayout {
            ivy("[module]/[revision]/teamcity-ivy.xml")
            artifact("[module]/[revision]/[artifact](.[ext])")
        }
    }
    if (!isStandaloneBuild) {
        maven("https://jetbrains.bintray.com/markdown")
    }
    maven("https://maven.google.com")
}

dependencies {
    addIdeaNativeModuleDeps(project)
    implementation(project(":kotlin-ultimate:ide:common-cidr-native"))
    implementation(project(":kotlin-ultimate:ide:common-cidr-swift-native"))
    implementation("com.jetbrains.intellij.cidr:cidr-cocoa:$cidrVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.cidr:cidr-cocoa-common:$cidrVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.cidr:cidr-xctest:$cidrVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$cidrVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.swift:swift:$cidrVersion") { isTransitive = false }
    if (ijProductBranch(cidrVersion) >= 202) {
        implementation("com.jetbrains.intellij.swift:swift-language:$cidrVersion") { isTransitive = false }
    }
    implementation(project(":kotlin-ultimate:ide:common-cidr-mobile"))
    implementation("com.jetbrains.intellij.android:android-kotlin-extensions-common:$cidrVersion") { isTransitive = false }
    implementation("com.android.tools.ddms:ddmlib:26.0.0") {
        exclude("com.google.guava", "guava")
    }
    implementation(project(":kotlin-ultimate:libraries:tools:apple-gradle-plugin-api"))

    testImplementation(testFixtures(project(":kotlin-ultimate:ide:common-cidr-swift-native")))
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xno-optimized-callable-references"
}

if (!isStandaloneBuild) {
    the<JavaPluginConvention>().sourceSets["test"].apply {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testResources"))
    }

    tasks.withType(Test::class.java).getByName("test") {
        workingDir = rootDir
    }

    Unit
}