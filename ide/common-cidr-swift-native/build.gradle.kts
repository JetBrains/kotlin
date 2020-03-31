import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.jk1.tcdeps") version "1.2"
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools

val isStandaloneBuild: Boolean by rootProject.extra

val cidrVersion: String by rootProject.extra
val kotlinNativeBackendVersion: String by rootProject.extra
val kotlinNativeBackendRepo: String by rootProject.extra

repositories {
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
    teamcityServer {
        setUrl("https://buildserver.labs.intellij.net")
    }
    if (!isStandaloneBuild) {
        maven("https://jetbrains.bintray.com/markdown")
    }
}

dependencies {
    addIdeaNativeModuleDeps(project)
    compile(project(":kotlin-ultimate:ide:common-cidr-native"))
    compileOnly("com.jetbrains.intellij.swift:swift:$cidrVersion")
    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa:$cidrVersion")
    compileOnly("com.jetbrains.intellij.platform:external-system-rt:$cidrVersion")
    compileOnly("com.esotericsoftware.kryo:kryo:2.24.0")

    testImplementation("com.jetbrains.intellij.swift:swift:$cidrVersion") {
        exclude("com.jetbrains.intellij.platform", "ide")
    }
    testImplementation("com.jetbrains.intellij.c:c:$cidrVersion") {
        exclude("com.jetbrains.intellij.platform", "ide")
    }

    implementation(tc("$kotlinNativeBackendRepo:$kotlinNativeBackendVersion:backend.native.jar"))
    testRuntime(tc("$kotlinNativeBackendRepo:${kotlinNativeBackendVersion}:konan.serializer.jar")) // required for backend.native
    testRuntime(files("${System.getProperty("java.home")}/../lib/tools.jar"))
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

if (!isStandaloneBuild) {
    dependencies {
        testCompile(project(":idea", configuration = "tests-jar"))
        testRuntime(project(":idea", configuration = "testRuntime"))
    }

    the<JavaPluginConvention>().sourceSets["test"].apply {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testResources"))
    }

    tasks.withType(Test::class.java).getByName("test") {
        dependsOn(":dist")
        workingDir = rootDir
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    kotlinOptions.freeCompilerArgs += "-Xskip-metadata-version-check"
}