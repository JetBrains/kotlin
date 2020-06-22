plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val proprietaryRepositories: Project.() -> Unit by ultimateTools
val cidrVersion: String by rootProject.extra
val ijProductBranch: (String) -> Int by ultimateTools

proprietaryRepositories(project)

addIdeaNativeModuleDeps(project)

dependencies {
    compileOnly("com.jetbrains.intellij.cidr:cidr-core:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa-common:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$cidrVersion") { isTransitive = false }
    api(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
