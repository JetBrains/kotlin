plugins {
    kotlin("jvm")
}

val clionUnscrambledJarDir: File by rootProject.extra
val clionVersion: String by rootProject.extra

val isStandaloneBuild: Boolean = rootProject.findProject(":idea") == null
val cacheRedirectorEnabled: Boolean = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

if (!isStandaloneBuild) {
    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/markdown")
        }
        maven("https://jetbrains.bintray.com/markdown")
    }
}
repositories {
    maven("https://maven.google.com")
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
}

dependencies {
    compile(project(":kotlin-ultimate:ide:common-cidr-native"))
    compile(project(":kotlin-ultimate:ide:common-cidr-swift-native"))
    compile("com.jetbrains.intellij.cidr:cidr-cocoa-common:$clionVersion") { isTransitive = false }
    compile("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$clionVersion") { isTransitive = false }
    compile("com.jetbrains.intellij.cidr:cidr-xctest:$clionVersion") { isTransitive = false }
    compileOnly(fileTree(clionUnscrambledJarDir) { include("**/*.jar") })
    compile("com.android.tools.ddms:ddmlib:26.0.0") {
        exclude("com.google.guava", "guava")
    }
    compile(project(":kotlin-ultimate:libraries:tools:apple-gradle-plugin-api"))
    compile("com.jetbrains.intellij.swift:swift:$clionVersion") { isTransitive = false }

    if (!isStandaloneBuild) {
        compileOnly("org.jetbrains:markdown:${rootProject.extra["versions.markdown"]}")
        val localDependencies = Class.forName("LocalDependenciesKt")
        val intellijDep = localDependencies
            .getMethod("intellijDep", Project::class.java, String::class.java)
            .invoke(null, project, null) as String
        compileOnly(intellijDep) {
            localDependencies
                .getMethod("includeJars", ModuleDependency::class.java, Array<String>::class.java, Project::class.java)
                .invoke(null, this, arrayOf("external-system-rt"), null)
        }
    }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}
