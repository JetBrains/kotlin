plugins {
    kotlin("jvm")
}

val cidrUnscrambledJarDir: File by rootProject.extra

dependencies {
    addIdeaNativeModuleDeps()
    compileOnly(fileTree(cidrUnscrambledJarDir) { include("**/*.jar") })
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
