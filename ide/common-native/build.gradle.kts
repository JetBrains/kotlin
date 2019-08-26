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

// TODO: don't use check for existence of `cidrUnscrambledJarDir` directory,
// it will give the wrong results after switching flags in local.properties
if (intellijBranch >= 192 || cidrUnscrambledJarDir?.exists() == true) {
    sourceSets["main"].java.setSrcDirs(listOf("src"))
    sourceSets["main"].resources.setSrcDirs(listOf("resources"))
} else {
    sourceSets["main"].java.setSrcDirs(emptyList<String>())
    sourceSets["main"].resources.setSrcDirs(emptyList<String>())
}

sourceSets["test"].java.setSrcDirs(emptyList<String>())
