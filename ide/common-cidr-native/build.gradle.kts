plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val proprietaryRepositories: Project.() -> Unit by ultimateTools
val cidrVersion: String by rootProject.extra

proprietaryRepositories(project)

addIdeaNativeModuleDeps(project)

dependencies {
    compileOnly(project(":kotlin-ultimate:ide:cidr-gradle-tooling")) { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-test-google:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.java:java-impl:$cidrVersion") { isTransitive = false }
    api(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
