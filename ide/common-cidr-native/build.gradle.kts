plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val cidrVersion: String by rootProject.extra

repositories {
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
}

dependencies {
    addIdeaNativeModuleDeps(project)
    compile(project(":kotlin-ultimate:ide:cidr-gradle-tooling"))
    compile(project(":kotlin-ultimate:ide:common-native"))
    compileOnly("com.jetbrains.intellij.cidr:cidr-test-google:$cidrVersion") { isTransitive = false }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
