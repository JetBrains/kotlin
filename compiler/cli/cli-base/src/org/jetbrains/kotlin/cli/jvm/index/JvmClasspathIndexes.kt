/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.JarArchiveCache
import org.jetbrains.kotlin.cli.jvm.index.nio.NioRootEntryFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.useJavaDirect
import org.jetbrains.kotlin.config.useNioJvmDependenciesIndex

/**
 * The classpath indexes of a compiler environment, extended together when roots are added during a compilation (e.g. by scripting).
 *
 * [javaFileManagerIndex] serves the PSI Java view and always has VFS entries. With NIO entries enabled, [classFinderIndex] is a separate
 * index over the same roots whose roots get NIO entries where available ([NioRootEntryFactory]); otherwise it is [javaFileManagerIndex].
 * The NIO entries of jars read the archives owned by the FastJar file system ([jarArchives]).
 */
class JvmClasspathIndexes(useNioEntries: Boolean, jarArchives: JarArchiveCache?) {

    /**
     * The indexes of an environment which reads jars with [jarFileSystem]. Jars get NIO entries only if that is FastJar; otherwise (e.g.
     * with `-Xuse-fast-jar-file-system=false`) they keep VFS entries, while directories and JRT still get NIO entries.
     */
    constructor(configuration: CompilerConfiguration, jarFileSystem: VirtualFileSystem?) : this(
        configuration.useNioJvmDependenciesIndex && configuration.useJavaDirect,
        (jarFileSystem as? FastJarFileSystem)?.archives,
    )

    val javaFileManagerIndex = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true)

    val classFinderIndex: JvmDependenciesDynamicCompoundIndex =
        if (useNioEntries) JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true) else javaFileManagerIndex

    private val nioRootEntryFactory = NioRootEntryFactory(jarArchives)

    /** Adds [roots] to both indexes and returns the index over them with VFS entries. */
    fun addRoots(roots: List<JavaRoot>): JvmDependenciesIndex {
        val index = JvmDependenciesIndexImpl(roots)
        javaFileManagerIndex.addIndex(index)
        if (classFinderIndex !== javaFileManagerIndex) classFinderIndex.addIndex(JvmDependenciesIndexImpl(roots, nioRootEntryFactory))
        return index
    }
}
