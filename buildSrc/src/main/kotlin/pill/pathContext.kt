@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

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

class ProjectContext(val project: PProject) : PathContext {
    override fun invoke(file: File): String {
        return file.absolutePath
            .replace(project.rootDirectory.absolutePath, "\$PROJECT_DIR\$")
    }
}

class ModuleContext(val project: PProject, val module: PModule) : PathContext {
    override fun invoke(file: File): String {
        if (!file.startsWith(project.rootDirectory)) {
            return file.absolutePath
        }

        return "\$MODULE_DIR\$/" + file.toRelativeString(module.rootDirectory)
    }
}