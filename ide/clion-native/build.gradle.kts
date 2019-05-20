plugins {
    kotlin("jvm")
}

val clionUnscrambledJarDir: File by rootProject.extra

val includeKotlinUltimate: Boolean = findProperty("includeKotlinUltimate")?.toString()?.toBoolean() == true
val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

if (includeKotlinUltimate) {
    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/markdown")
        }
        maven("https://jetbrains.bintray.com/markdown")
    }
}

dependencies {
    compile(project(":kotlin-ultimate:ide:cidr-native"))
    compileOnly(fileTree(clionUnscrambledJarDir) { include("**/*.jar") })

    if (includeKotlinUltimate) {
        compileOnly("org.jetbrains:markdown:${rootProject.extra["versions.markdown"]}")
    }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
