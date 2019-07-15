plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addCidrDeps: (Project) -> Unit by ultimateTools
val addIdeaNativeModuleDepsComposite: (Project) -> Unit by ultimateTools

dependencies {
    addCidrDeps(project)
    addIdeaNativeModuleDepsComposite(project)
    compile(project(":kotlin-native:kotlin-native-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}