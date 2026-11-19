/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import org.jetbrains.kotlin.cli.jvm.compiler.JvmModuleMappings
import org.jetbrains.kotlin.cli.jvm.index.nio.JarFilePool
import org.jetbrains.kotlin.cli.jvm.index.nio.NioRootEntryFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.useJavaDirect
import org.jetbrains.kotlin.config.useNioJvmDependenciesIndex
import java.io.Closeable

/**
 * The classpath indexes of a compiler environment, extended together when roots are added during a compilation (e.g. by scripting).
 *
 * [javaFileManagerIndex] serves the PSI Java view and always has VFS entries. With NIO entries enabled, [classFinderIndex] is a separate
 * index over the same roots whose roots get NIO entries where available ([NioRootEntryFactory]); otherwise it is [javaFileManagerIndex].
 * The archives of the NIO entries belong to [jarPool], which [close] releases; the owner closes the indexes when its environment is
 * disposed. Handles stay usable afterwards: a later read opens its archive again.
 */
class JvmClasspathIndexes(
    useNioEntries: Boolean,
    languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
) : Closeable {

    constructor(configuration: CompilerConfiguration) : this(
        configuration.useNioJvmDependenciesIndex &&
                configuration.useJavaDirect &&
                configuration[JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM] != false,
        configuration.languageVersionSettings,
    )

    val javaFileManagerIndex = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true)

    val classFinderIndex: JvmDependenciesDynamicCompoundIndex =
        if (useNioEntries) JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true) else javaFileManagerIndex

    /** The archives of the NIO entries; `null` without NIO entries or if mapped archives cannot be released on this JVM. */
    val jarPool: JarFilePool? = if (useNioEntries) JarFilePool.createIfUnmappingPossible() else null

    private val nioRootEntryFactory = NioRootEntryFactory(jarPool)

    /** The entries of the roots of [classFinderIndex], also for reading root content outside the index (e.g. module descriptors). */
    val rootEntryFactory: JvmRootEntryFactory = if (useNioEntries) nioRootEntryFactory else JvmRootEntryFactory.VFS

    /** The `.kotlin_module` mappings of the roots, read through [rootEntryFactory]. */
    val moduleMappings = JvmModuleMappings(languageVersionSettings, rootEntryFactory)

    /** Adds [roots] to both indexes and returns the index over them with VFS entries. */
    fun addRoots(roots: List<JavaRoot>): JvmDependenciesIndex {
        val index = JvmDependenciesIndexImpl(roots)
        javaFileManagerIndex.addIndex(index)
        if (classFinderIndex !== javaFileManagerIndex) classFinderIndex.addIndex(JvmDependenciesIndexImpl(roots, nioRootEntryFactory))
        return index
    }

    override fun close() {
        jarPool?.close()
    }
}
