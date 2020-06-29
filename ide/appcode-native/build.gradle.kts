import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Locale

plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val ijProductBranch: (String) -> Int by ultimateTools
val disableBuildTasks: Project.(String) -> Unit by ultimateTools
val proprietaryRepositories: Project.() -> Unit by ultimateTools

val appcodeVersion: String by rootProject.extra
val appcodeUnscrambledJarDir: File by rootProject.extra

proprietaryRepositories(project)

addIdeaNativeModuleDeps(project)

dependencies {
    compileOnly(fileTree(appcodeUnscrambledJarDir) { include("**/*.jar") })
    api(project(":kotlin-ultimate:ide:common-cidr-swift-native"))
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

if (ijProductBranch(appcodeVersion) < 201)
    disableBuildTasks("Too old AppCode version: $appcodeVersion")
else
    System.getProperty("os.name")!!.toLowerCase(Locale.US).takeIf { "windows" in it }?.let {
        disableBuildTasks("Can't build AppCode plugin under Windows")
    }

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
}