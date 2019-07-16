import java.util.*
import kotlin.reflect.KFunction

// --------------------------------------------------
// Exported items:
// --------------------------------------------------

// TODO: pack this as a Gradle plugin
val ultimateTools: Map<String, KFunction<Any>> = listOf<KFunction<Any>>(
    ::enableTasksIfAtLeast,
    ::enableTasksIfOsIsNot,
    ::addCidrDeps,
    ::addIdeaNativeModuleDepsComposite
).map { it.name to it }.toMap()

rootProject.extensions.add("ultimateTools", ultimateTools)

// --------------------------------------------------
// Compatibility tasks:
// --------------------------------------------------

fun enableTasksIfAtLeast(project: Project, productVersion: String, expectedProductBranch: Int) = with(project) {
    val productBranch = productVersion.substringBefore('.').toIntOrNull()
        ?: error("Invalid product version format: $productVersion")

    if (productBranch >= expectedProductBranch)
        return // OK, nothing to disable

    // otherwise: disable build tasks
    disableBuildTasks { "$productVersion is NOT at least $expectedProductBranch" }
}

fun enableTasksIfOsIsNot(project: Project, osNames: List<String>) = with(project) {
    osNames.forEach { osName ->
        if (osName.isBlank() || osName.trim() != osName)
            error("Invalid OS name: $osName")
    }

    val hostOsName = System.getProperty("os.name")!!.toLowerCase(Locale.US)

    if (osNames.any { it.toLowerCase(Locale.US) in hostOsName }) {
        disableBuildTasks { "\"$hostOsName\" is NOT one of ${osNames.joinToString { "\"$it\"" }}" }
    }
}

// disable anything but "clean" and tasks from "help" group
// log the appropriate message
fun Project.disableBuildTasks(message: () -> String) {
    val tasksToDisable = tasks.filter {
        it.enabled && it.name != "clean" && it.group != "help"
    }

    if (tasksToDisable.isNotEmpty()) {
        tasksToDisable.forEach { it.enabled = false }
        logger.warn("Build tasks in $project have been disabled due to condition mismatch: ${message()}: ${tasksToDisable.joinToString { it.name }}")
    }
}

// --------------------------------------------------
// Shared utils:
// --------------------------------------------------

val javaApiArtifacts: List<String> by rootProject.extra

val cidrPluginsEnabled: Boolean? by rootProject.extra

fun addIdeaNativeModuleDepsComposite(project: Project) = with(project) {
    dependencies {
        // Gradle projects with Kotlin/Native-specific logic
        // (automatically brings all the necessary transient dependencies, include deps on IntelliJ platform)
        add("compile", project(":idea:idea-native"))
        add("compile", project(":idea:idea-gradle-native"))

        // Detect IDE name and version
        // TODO: add dependency on base project artifacts
        val intellijUltimateEnabled: Boolean? by rootProject.extra
        val ideName = if (intellijUltimateEnabled == true) "ideaIU" else "ideaIC" // TODO: what if AndroidStudio?
        val ideVersion = rootProject.extra["versions.intellijSdk"] as String
        val ideBranch = ideVersion.substringBefore('.').toIntOrNull() ?: error("Invalid product version format: $ideVersion")
        val javaModuleName = if (ideBranch >= 192) "java" else ideName

        add("compile", "kotlin.build:$javaModuleName:$ideVersion") {
            javaApiArtifacts.forEach { jarName ->
                artifact {
                    name = jarName
                    type = "jar"
                    extension = "jar"
                }
            }
            isTransitive = false
        }

        val ijPlatformDependencies = listOf(
            "idea",
            "openapi",
            "platform-api",
            "platform-impl",
            "util",
            "extensions",
            "jdom"
        ) + if (ideBranch >= 192)
            listOf("intellij-dvcs", "platform-util-ui", "platform-util-ex")
        else
            emptyList()

        add("compile", "kotlin.build:$ideName:$ideVersion") {
            ijPlatformDependencies.forEach { jarName ->
                artifact {
                    name = jarName
                    type = "jar"
                    extension = "jar"
                }
            }
            isTransitive = false
        }
    }
}

fun addCidrDeps(project: Project) = with(project) {
    dependencies {
        val cidrUnscrambledJarDir: File? by rootProject.extra
        val nativeDebugPluginDir: File? by rootProject.extra
        if (cidrUnscrambledJarDir?.exists() == true) { // CIDR build
            add("compile", fileTree(cidrUnscrambledJarDir) { include("**/*.jar") })
        } else if (nativeDebugPluginDir?.exists() == true) { // Idea Ultimate build
            add("compile", fileTree(nativeDebugPluginDir) { include("**/*.jar") })
        }
    }
}

