plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val proprietaryRepositories: Project.() -> Unit by ultimateTools
val cidrVersion: String by rootProject.extra

proprietaryRepositories(project)

dependencies {
    addIdeaNativeModuleDeps(project)
    compileOnly(project(":kotlin-ultimate:ide:cidr-gradle-tooling")) { isTransitive = false }
    compileOnly(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-test-google:$cidrVersion") { isTransitive = false }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
