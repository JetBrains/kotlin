plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addCidrDeps: (Project) -> Unit by ultimateTools

addCidrDeps(project)

dependencies {
}

the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
}


