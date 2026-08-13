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
 * Deliberately not a [Path]: a root does not have to be a location in a file system. A build system may
 * compile against the output of the previous build without ever writing it to disk — the IntelliJ build
 * system does, registering it as a `VirtualJvmClasspathRoot` over a file system of its own — and such a root
 * has no [Path] that any file system could resolve. Everything a root is used for is comparing it with
 * another root and recognising the files under it, and both are comparisons of [id].
 *
 * [id] is the path of the root as a virtual file system spells it: absolute, `/`-separated, without a
 * trailing separator and without the `!/` that marks the inside of an archive. That is exactly the prefix of
 * the path of every file under the root, which is what makes both comparisons possible without asking a file
 * system anything.
 */
@JvmInline
value class JvmClasspathRootId(val id: String) {
    override fun toString(): String = id

    companion object {
        fun of(root: Path): JvmClasspathRootId =
            JvmClasspathRootId(root.toAbsolutePath().normalize().invariantSeparatorsPathString.trimEnd('/'))
    }
}

/**
 * The identity of a root which is already resolved to a virtual file. Kept next to [JvmClasspathRootId.of],
 * because the two have to agree: an archive root is `.../lib.jar` as a path and `.../lib.jar!/` as a virtual
 * file.
 */
fun VirtualFile.asJvmClasspathRootId(): JvmClasspathRootId =
    JvmClasspathRootId(path.removeSuffix(JAR_SEPARATOR).trimEnd('/'))
