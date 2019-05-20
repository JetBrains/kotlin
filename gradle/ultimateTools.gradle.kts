import java.util.*

// --------------------------------------------------
// Exported items:
// --------------------------------------------------

val ultimateTools: MutableMap<String, Any> by rootProject.extra(mutableMapOf())

ultimateTools["enableTasksIfAtLeast"] = ::enableTasksIfAtLeast
ultimateTools["enableTasksIfOsIsNot"] = ::enableTasksIfOsIsNot

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
