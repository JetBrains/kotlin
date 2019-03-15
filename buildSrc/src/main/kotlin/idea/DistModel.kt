package org.jetbrains.kotlin.buildUtils.idea

import org.gradle.api.Action
import org.gradle.api.file.FileCopyDetails
import java.io.File
import java.io.PrintWriter


class DistVFile(
    val parent: DistVFile?,
    val name: String,
    val file: File = File(parent!!.file, name)
) {
    val child = mutableMapOf<String, DistVFile>()

    val contents = mutableSetOf<DistContentElement>()

    override fun toString(): String = name

    val hasContents: Boolean = file.exists() || contents.isNotEmpty()

    fun relativePath(path: String): DistVFile {
        val pathComponents = path.split(File.separatorChar)
        return pathComponents.fold(this) { parent: DistVFile, childName: String ->
            try {
                parent.getOrCreateChild(childName)
            } catch (t: Throwable) {
                throw Exception("Error while processing path `$path`, components: `$pathComponents`, element: `$childName`", t)
            }
        }
    }

    fun getOrCreateChild(name: String): DistVFile = child.getOrPut(name) {
        DistVFile(this, name)
    }

    fun addContents(contents: DistContentElement) {
        this.contents.add(contents)
    }

    fun removeAll(matcher: (String) -> Boolean) {
        child.keys.filter(matcher).forEach {
            child.remove(it)
        }
    }

    fun printTree(p: PrintWriter, depth: String = "") {
        p.println("$depth${file.path} ${if (file.exists()) "EXISTED" else ""}:")
        contents.forEach {
            p.println("$depth  $it")
        }
        child.values.forEach {
            it.printTree(p, "$depth  ")
        }
    }
}

sealed class DistContentElement()

class DistCopy(
    target: DistVFile,
    val src: DistVFile,
    val customTargetName: String? = null,
    val copyActions: Collection<Action<in FileCopyDetails>> = listOf()
) : DistContentElement() {
    init {
        target.addContents(this)
    }

    override fun toString(): String =
        "COPY OF ${src.file}" +
                if (customTargetName != null) " -> $customTargetName" else ""
}

class DistModuleOutput(parent: DistVFile, val projectId: String) : DistContentElement() {
    init {
        parent.addContents(this)
    }

    override fun toString(): String = "COMPILE OUTPUT $projectId"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DistModuleOutput

        if (projectId != other.projectId) return false

        return true
    }

    override fun hashCode(): Int {
        return projectId.hashCode()
    }
}