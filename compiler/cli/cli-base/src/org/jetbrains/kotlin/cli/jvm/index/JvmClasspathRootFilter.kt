/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.asJvmClasspathRootId
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches classpath acceptance by indexed root, rather than matching every class file against the classpath.
 *
 * Acceptance applies to the whole indexed root. A classpath entry nested inside a broader indexed root does not make only that
 * subdirectory visible.
 */
class JvmClasspathRootFilter(classpath: JvmClasspath) : ClassFileFilter {
    private val matcher = JvmClasspathRootMatcher(classpath)
    private val acceptedRoots = ConcurrentHashMap<VirtualFile, Boolean>()

    override fun accepts(root: VirtualFile, file: VirtualFile): Boolean =
        acceptedRoots.computeIfAbsent(root, matcher::accepts)
}

/**
 * Matches indexed roots against classpath entries. Directory roots use component-aware path containment, archive roots match only
 * the classpath entry naming that archive, and other roots use their string identity. Matching is lexical: `..` segments are not
 * resolved and symbolic links are not followed.
 */
internal class JvmClasspathRootMatcher(private val classpath: JvmClasspath) {
    private val rootIds: List<JvmClasspathRootId> = when (classpath) {
        is JvmClasspath.Roots -> classpath.roots
        is JvmClasspath.ProjectLibraries -> classpath.excludedRoots
    }

    private val rootPaths: LinkedHashSet<Path> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        rootIds.mapNotNullTo(LinkedHashSet()) { parsePath(it.id) }
    }

    fun accepts(root: VirtualFile): Boolean = when (classpath) {
        is JvmClasspath.Roots -> isAtOrUnderNamedRoot(root)
        is JvmClasspath.ProjectLibraries -> !isAtOrUnderNamedRoot(root)
    }

    private fun isAtOrUnderNamedRoot(root: VirtualFile): Boolean {
        if (rootIds.isEmpty()) return false

        // Local roots can be compared directly to the cached NIO paths of the classpath entries.
        root.fileSystem.getNioPath(root)?.let { rootPath ->
            return rootPaths.any(rootPath::startsWith)
        }

        // An archive is a root only when the VFS file is the archive itself, not an entry inside it.
        root.archivePath()?.let { archivePath ->
            return archivePath in rootPaths
        }

        return root.asJvmClasspathRootId() in rootIds
    }
}

private fun VirtualFile.archivePath(): Path? {
    if (fileSystem.protocol != StandardFileSystems.JAR_PROTOCOL) return null

    val path = path
    val archiveSeparator = path.indexOf(JAR_SEPARATOR)
    if (archiveSeparator < 0 || archiveSeparator + JAR_SEPARATOR.length != path.length) return null
    return parsePath(path.substring(0, archiveSeparator))
}

private fun parsePath(path: String): Path? = try {
    Paths.get(path)
} catch (_: InvalidPathException) {
    null
}

/**
 * Returns whether [root] is included by [JvmClasspath.Roots] or is not among the excluded roots of
 * [JvmClasspath.ProjectLibraries].
 */
fun JvmClasspath.containsRoot(root: VirtualFile): Boolean = JvmClasspathRootMatcher(this).accepts(root)
