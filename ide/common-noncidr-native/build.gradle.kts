plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val intellijBranch: Int by rootProject.extra
val nativeDebugPluginDir: File? by rootProject.extra

dependencies {
    compileOnly(kotlinStdlib("jdk8"))
    compileOnly(project(":idea:idea-gradle")) { isTransitive = false }
    compileOnly(project(":idea:kotlin-gradle-tooling")) { isTransitive = false }
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
            "idea",
            "platform-ide-util-io",
            "platform-util-ui"
        ) }
    }
    if (nativeDebugPluginDir != null) {
        compileOnly(fileTree(nativeDebugPluginDir!!) { include("**/*.jar") })
    }

    api(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
}

if (intellijBranch >= 193) {
    sourceSets["main"].java.setSrcDirs(listOf("src"))
    sourceSets["main"].resources.setSrcDirs(listOf("resources"))
} else {
    sourceSets["main"].java.setSrcDirs(emptyList<String>())
    sourceSets["main"].resources.setSrcDirs(emptyList<String>())
}

sourceSets["test"].java.setSrcDirs(emptyList<String>())


