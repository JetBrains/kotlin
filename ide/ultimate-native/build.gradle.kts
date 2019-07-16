plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addCidrDeps: (Project) -> Unit by ultimateTools
val cidrUnscrambledJarDir: File? by rootProject.extra

dependencies {
    compile(kotlinStdlib("jdk8"))
    compile(project(":compiler:cli-common")) { isTransitive = false }
    compile(project(":core:descriptors")) { isTransitive = false }
    compile(project(":idea:kotlin-gradle-tooling")) { isTransitive = false }
    compile(project(":idea:idea-core")) { isTransitive = false }
    compile(project(":idea:idea-jps-common")) { isTransitive = false }
    compile(project(":idea:idea-jvm")) { isTransitive = false }
    compile(project(":idea:idea-gradle")) { isTransitive = false }
    compile(project(":kotlin-util-io")) { isTransitive = false }
    compile(project(":kotlin-native:kotlin-native-utils")) { isTransitive = false }
    compile(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijUltimateDep()) { includeJars("platform-api", "platform-impl", "util", "jdom") }
    compileOnly(intellijUltimatePluginDep("gradle"))
    compileOnly(intellijUltimatePluginDep("Groovy"))
    compileOnly(intellijUltimatePluginDep("java")) { includeJars("java-impl") }
    addCidrDeps(project)

    Platform[192].orHigher {
        compileOnly(intellijUltimateDep()) { includeJars("platform-util-ui") }
    }
}

sourceSets {
    if (Ide.IJ192.orHigher() || cidrUnscrambledJarDir?.exists() == true) {
        "main" { projectDefault() }
    } else {
        "main" {}
    }
    "test" {}
}