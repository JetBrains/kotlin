plugins {
    kotlin("jvm")
}

val isStandaloneBuild: Boolean by rootProject.extra

val ultimateTools: Map<String, Any> by rootProject.extensions
val ijProductBranch: (String) -> Int by ultimateTools
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools

val cidrVersion: String by rootProject.extra
val cidrUnscrambledJarDir: File by rootProject.extra

repositories {
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
}

dependencies {
    compile(project(":kotlin-ultimate:ide:cidr-gradle-tooling"))
    compile(project(":kotlin-ultimate:ide:common-cidr-native"))
    compileOnly(fileTree(cidrUnscrambledJarDir) { include("**/*.jar") })
    if (!isStandaloneBuild && ijProductBranch(cidrVersion) >= 193) {
        compileOnly("com.jetbrains.intellij.cidr:cidr-test-google:$cidrVersion")
    }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
