@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import org.gradle.api.Project
import java.io.File

interface PathContext {
    operator fun invoke(file: File): String

    fun url(file: File): Pair<String, String> {
        val path = when {
            file.isFile && file.extension.toLowerCase() == "jar" -> "jar://" + this(file) + "!/"
            else -> "file://" + this(file)
        }

        return Pair("url", path)
    }
}

class ProjectContext private constructor(val projectDir: File) : PathContext {
    constructor(project: PProject) : this(project.rootDirectory)
    constructor(project: Project) : this(project.projectDir)

    override fun invoke(file: File): String {
        return file.absolutePath.replace(projectDir.absolutePath, "\$PROJECT_DIR\$")
    }
}

class ModuleContext(val project: PProject, val module: PModule) : PathContext {
    override fun invoke(file: File): String {
        if (!file.startsWith(project.rootDirectory)) {
            return file.absolutePath
        }

        return "\$MODULE_DIR\$/" +
            project.rootDirectory.toRelativeString(module.moduleFile.parentFile).withSlash() +
            module.rootDirectory.toRelativeString(project.rootDirectory).withSlash() +
            file.toRelativeString(module.rootDirectory)
    }
}

fun String.withSlash() = if (this.endsWith("/")) this else (this + "/")
fun String.withoutSlash() = this.trimEnd('/')