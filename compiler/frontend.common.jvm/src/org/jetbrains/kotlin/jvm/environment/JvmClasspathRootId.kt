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
 * trailing separator and without the archive separator `!/`.
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
