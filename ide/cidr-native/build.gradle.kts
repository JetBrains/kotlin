plugins {
    kotlin("jvm")
}

val cidrPluginTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by cidrPluginTools

val cidrUnscrambledJarDir: File by rootProject.extra

dependencies {
    addIdeaNativeModuleDeps(project)
    compile(project(":kotlin-ultimate:ide:common-native"))
    compile(project(":idea:kotlin-gradle-tooling"))
    compileOnly(fileTree(cidrUnscrambledJarDir) { include("**/*.jar") })
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
