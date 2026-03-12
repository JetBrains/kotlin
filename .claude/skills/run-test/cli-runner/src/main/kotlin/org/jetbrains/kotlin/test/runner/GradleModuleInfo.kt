@file:Suppress("MatchingDeclarationName")

package org.jetbrains.kotlin.test.runner

import java.nio.file.Path

data class GradleModuleInfo(
    val modulePath: String,
    val taskName: String,
)

fun resolveGradleModule(
    classFilePath: Path,
    projectRoot: Path,
): GradleModuleInfo {
    val relativePath = projectRoot.relativize(classFilePath).toString()
    val parts = relativePath.split("/")

    val buildIndex = parts.indexOf("build")
    require(buildIndex >= 0 && buildIndex + 3 < parts.size && parts[buildIndex + 1] == "classes") {
        "Unexpected class file path structure: $relativePath"
    }

    val moduleParts = parts.subList(0, buildIndex)
    val modulePath = if (moduleParts.isEmpty()) ":" else ":" + moduleParts.joinToString(":")

    // Path structure: <module>/build/classes/<lang>/<sourceSet>/...
    val sourceSet = parts[buildIndex + 3]

    return GradleModuleInfo(
        modulePath = modulePath,
        taskName = sourceSet,
    )
}
