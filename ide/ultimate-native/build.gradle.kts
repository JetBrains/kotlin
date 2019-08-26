plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addCidrDeps: (Project) -> Unit by ultimateTools
val cidrUnscrambledJarDir: File? by rootProject.extra
val intellijBranch: Int by rootProject.extra

dependencies {
    compile(kotlinStdlib("jdk8"))
    compile(project(":compiler:cli-common")) { isTransitive = false }
    compile(project(":compiler:psi")) { isTransitive = false }
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

    if (intellijBranch >= 192) {
        compileOnly(intellijUltimateDep()) { includeJars("platform-util-ui") }
    }
}

// TODO: don't use check for existence of `cidrUnscrambledJarDir` directory,
// it will give the wrong results after switching flags in local.properties
if (intellijBranch >= 192 || cidrUnscrambledJarDir?.exists() == true) {
    sourceSets["main"].java.setSrcDirs(listOf("src"))
} else {
    sourceSets["main"].java.setSrcDirs(emptyList<String>())
}

sourceSets["test"].java.setSrcDirs(emptyList<String>())
