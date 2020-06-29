plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addIdeaNativeModuleDeps: (Project) -> Unit by ultimateTools
val intellijBranch: Int by rootProject.extra

addIdeaNativeModuleDeps(project)

if (intellijBranch >= 192) {
    sourceSets["main"].java.setSrcDirs(listOf("src"))
    sourceSets["main"].resources.setSrcDirs(listOf("resources"))
} else {
    sourceSets["main"].java.setSrcDirs(emptyList<String>())
    sourceSets["main"].resources.setSrcDirs(emptyList<String>())
}

sourceSets["test"].java.setSrcDirs(emptyList<String>())