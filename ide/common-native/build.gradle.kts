plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addCidrDeps: (Project) -> Unit by ultimateTools
val addIdeaNativeModuleDepsComposite: (Project) -> Unit by ultimateTools
val cidrUnscrambledJarDir: File? by rootProject.extra

dependencies {
    addCidrDeps(project)
    addIdeaNativeModuleDepsComposite(project)
    compile(project(":kotlin-native:kotlin-native-utils"))
}

sourceSets {
    if (Ide.IJ192.orHigher() || cidrUnscrambledJarDir?.exists() == true) {
        "main" { projectDefault() }
    } else {
        "main" {}
    }
    "test" {}
}