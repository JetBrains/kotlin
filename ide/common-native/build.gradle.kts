plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addCidrDeps: (Project) -> Unit by ultimateTools
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val cidrUnscrambledJarDir: File? by rootProject.extra
val intellijBranch: Int by rootProject.extra

dependencies {
    addCidrDeps(project)
    addIdeaNativeModuleDeps(project)
}

if (intellijBranch >= 192 || cidrUnscrambledJarDir?.exists() == true) {
    sourceSets["main"].java.setSrcDirs(listOf("src"))
} else {
    sourceSets["main"].java.setSrcDirs(emptyList<String>())
}

sourceSets["test"].java.setSrcDirs(emptyList<String>())
