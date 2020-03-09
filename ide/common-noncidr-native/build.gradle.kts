plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addCidrDeps: (Project) -> Unit by ultimateTools
val intellijBranch: Int by rootProject.extra

addCidrDeps(project)

dependencies {
    compileOnly(kotlinStdlib("jdk8"))
    compileOnly(project(":idea:idea-gradle")) { isTransitive = false }
    compileOnly(project(":idea:kotlin-gradle-tooling")) { isTransitive = false }
    compileOnly(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    compileOnly(project(":kotlin-util-io")) { isTransitive = false }
    compileOnly(project(":native:kotlin-native-utils")) { isTransitive = false }
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijUltimatePluginDep("java")) { includeJars("java-api", "java-impl") }
    compileOnly(intellijDep()) { includeJars(
        "jdom",
        "platform-api",
        "platform-impl",
        "util"
    ) }
    if (intellijBranch >= 193) {
        compileOnly(intellijDep()) { includeJars(
            "external-system-rt",
            "platform-ide-util-io",
            "platform-util-ui"
        ) }
    }
}

if (intellijBranch >= 193) {
    sourceSets["main"].java.setSrcDirs(listOf("src"))
} else {
    sourceSets["main"].java.setSrcDirs(emptyList<String>())
}

sourceSets["test"].java.setSrcDirs(emptyList<String>())


