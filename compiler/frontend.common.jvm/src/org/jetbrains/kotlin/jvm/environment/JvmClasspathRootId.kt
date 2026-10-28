/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.environment

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

/**
 * The identity of one JVM classpath root — a directory or a `.jar`/`.jmod` file, as it is spelled on the
 * compiler's classpath.
 *
 * [id] is the path of the root as a virtual file system spells it: absolute, `/`-separated, without a
 * trailing separator and without the archive separator `!/`. A root which shares its path with other roots
 * (the JDK `ct.sym` roots of `-Xjdk-release`) gets a synthetic id instead; such a root never appears on a
 * user's classpath, so it only ever has to be unique.
 */
@JvmInline
value class JvmClasspathRootId(val id: String) {
    override fun toString(): String = id

    companion object {
        fun of(root: Path): JvmClasspathRootId =
            JvmClasspathRootId(root.toAbsolutePath().normalize().invariantSeparatorsPathString.trimEnd('/'))
    }
}

fun VirtualFile.asJvmClasspathRootId(): JvmClasspathRootId =
    JvmClasspathRootId(path.removeSuffix(JAR_SEPARATOR).trimEnd('/'))

fun VirtualFile.isUnder(classpathRoot: JvmClasspathRootId): Boolean {
    val root = classpathRoot.id
    val path = path
    val archiveSeparator = path.indexOf(JAR_SEPARATOR)
    return when {
        archiveSeparator >= 0 -> archiveSeparator == root.length && path.startsWith(root)
        else -> path.length > root.length && path[root.length] == '/' && path.startsWith(root)
    }
}

fun VirtualFile.isClasspathRootAtOrUnder(classpathRoot: JvmClasspathRootId): Boolean =
    asJvmClasspathRootId() == classpathRoot || isUnder(classpathRoot)

fun JvmClasspath.containsFile(file: VirtualFile): Boolean = when (this) {
    is JvmClasspath.Roots -> roots.any(file::isUnder)
    is JvmClasspath.ProjectLibraries -> excludedRoots.none(file::isUnder)
}

fun JvmClasspath.containsRoot(root: VirtualFile): Boolean = when (this) {
    is JvmClasspath.Roots -> roots.any(root::isClasspathRootAtOrUnder)
    is JvmClasspath.ProjectLibraries -> excludedRoots.none(root::isClasspathRootAtOrUnder)
}
