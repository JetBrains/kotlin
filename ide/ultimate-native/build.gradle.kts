plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val intellijBranch: Int by rootProject.extra
val nativeDebugPluginDir: File? by rootProject.extra

dependencies {
    compileOnly(kotlinStdlib("jdk8"))
    compileOnly(project(":compiler:cli-common")) { isTransitive = false }
    compileOnly(project(":compiler:psi")) { isTransitive = false }
    compileOnly(project(":core:compiler.common")) { isTransitive = false }
    compileOnly(project(":core:descriptors")) { isTransitive = false }
    compileOnly(project(":idea:kotlin-gradle-tooling")) { isTransitive = false }
    compileOnly(project(":idea:idea-core")) { isTransitive = false }
    compileOnly(project(":idea:idea-jps-common")) { isTransitive = false }
    compileOnly(project(":idea:idea-jvm")) { isTransitive = false }
    compileOnly(project(":idea:idea-gradle")) { isTransitive = false }
    compileOnly(project(":kotlin-util-io")) { isTransitive = false }
    compileOnly(project(":native:kotlin-native-utils")) { isTransitive = false }
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijUltimateDep()) { includeJars("platform-api", "platform-impl", "util", "jdom") }
    compileOnly(intellijUltimatePluginDep("gradle"))
    compileOnly(intellijUltimatePluginDep("Groovy"))
    compileOnly(intellijUltimatePluginDep("java")) { includeJars("java-api", "java-impl") }
    if (nativeDebugPluginDir != null) {
        compileOnly(fileTree(nativeDebugPluginDir!!) { include("**/*.jar") })
    }


    if (intellijBranch >= 192) {
        compileOnly(intellijUltimateDep()) { includeJars("platform-util-ui") }
        if (intellijBranch >= 193) {
            compileOnly(intellijUltimateDep()) { includeJars(
                "extensions",
                "external-system-rt",
                "platform-ide-util-io"
            ) }
        }
    }
    api(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    api(project(":kotlin-ultimate:ide:common-noncidr-native")) { isTransitive = false }
}

if (intellijBranch >= 192) {
    sourceSets["main"].java.setSrcDirs(listOf("src"))
} else {
    sourceSets["main"].java.setSrcDirs(emptyList<String>())
}

sourceSets["test"].java.setSrcDirs(emptyList<String>())
