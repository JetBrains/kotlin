import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val addKotlinNativeDeps: (Project, String) -> Unit by ultimateTools

val isStandaloneBuild: Boolean by rootProject.extra

val cidrVersion: String by rootProject.extra

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
}

dependencies {
    addIdeaNativeModuleDeps(project)
    compile(project(":kotlin-ultimate:ide:common-cidr-native"))
    compile(project(":kotlin-ultimate:ide:cidr-gradle-tooling"))
    compileOnly("com.jetbrains.intellij.swift:swift:$cidrVersion")
    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa:$cidrVersion")
    compileOnly("com.esotericsoftware.kryo:kryo:2.24.0")
    compileOnly("org.jetbrains.intellij.deps:trove4j:1.0.20200330")

    testFixturesApi("com.jetbrains.intellij.swift:swift:$cidrVersion") {
        exclude("com.jetbrains.intellij.platform", "ide")
    }
    testFixturesApi("com.jetbrains.intellij.c:c:$cidrVersion") {
        exclude("com.jetbrains.intellij.platform", "ide")
    }

    addKotlinNativeDeps(project, "compileOnly")
    addKotlinNativeDeps(project, "testFixturesApi")

    testFixturesApi(files("${System.getProperty("java.home")}/../lib/tools.jar"))
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

if (!isStandaloneBuild) {
    dependencies {
        testFixturesApi(project(":idea", configuration = "tests-jar"))
        testFixturesApi(project(":idea", configuration = "testRuntime"))
    }

    the<JavaPluginConvention>().sourceSets["test"].apply {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testResources"))
    }

    the<JavaPluginConvention>().sourceSets["testFixtures"].apply {
        java.setSrcDirs(listOf("testFixtures"))
    }

    tasks.withType(Test::class.java).getByName("test") {
        workingDir = rootDir
    }

    tasks.withType(Jar::class.java).getByName("testFixturesJar") {
        dependsOn(":dist")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    kotlinOptions.freeCompilerArgs += "-Xskip-metadata-version-check"
    kotlinOptions.freeCompilerArgs += "-Xno-optimized-callable-references"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}