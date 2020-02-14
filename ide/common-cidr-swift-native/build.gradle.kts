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
val useAppCodeForCommon: Boolean by rootProject.extra

val cidrVersion: String by rootProject.extra
val cidrUnscrambledJarDir: File by rootProject.extra
val kotlinNativeBackendVersion: String by rootProject.extra

repositories {
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
    teamcityServer {
        setUrl("https://buildserver.labs.intellij.net")
    }
}

dependencies {
    addIdeaNativeModuleDeps(project)
    compile(project(":kotlin-ultimate:ide:common-cidr-native"))
    compileOnly(fileTree(cidrUnscrambledJarDir) { include("**/*.jar") })
    if (!isStandaloneBuild || !useAppCodeForCommon) {
        compileOnly("com.jetbrains.intellij.swift:swift:$cidrVersion") { isTransitive = false }
        compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa-common:$cidrVersion") { isTransitive = false }
        compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa:$cidrVersion") { isTransitive = false }
    }
    compileOnly(tc("Kotlin_KotlinNative_Master_KotlinNativeLinuxBundle:${kotlinNativeBackendVersion}:backend.native.jar"))

    if (!isStandaloneBuild) {
        val localDependencies = Class.forName("LocalDependenciesKt")
        val intellijDep = localDependencies
            .getMethod("intellijDep", Project::class.java, String::class.java)
            .invoke(null, project, null) as String
        compileOnly(intellijDep) {
            localDependencies
                .getMethod("includeJars", ModuleDependency::class.java, Array<String>::class.java, Project::class.java)
                .invoke(null, this, arrayOf("trove4j", "external-system-rt", "objenesis-3.0.1", "kryo-2.24.0"), null)
        }
    }
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
}