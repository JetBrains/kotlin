/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.core.CoreJavaFileManager
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.useJavaDirect
import org.jetbrains.kotlin.fir.session.FirJavaInterop
import org.jetbrains.kotlin.java.direct.JavaSourceRootEntry
import org.jetbrains.kotlin.java.direct.createJavaDirectJavaInterop
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileIndex
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClassCache
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder

/**
 * The Java view of this compilation as [configuration] asks for it: the java-direct one under
 * `-Xjava-direct`, the PSI one otherwise. The two peers — [createJavaDirectJavaInterop] and
 * [psiJavaInterop] — stay peers; this is only the one place which knows how each of them is built, so
 * that every JVM-hosted pipeline follows the same flag instead of deciding on its own.
 *
 * [withJavaSources] says whether this compilation has `.java` sources of its own at all. It is a single
 * switch because the two implementations describe those sources differently — java-direct by source root,
 * PSI by search scope — and a caller must not be able to tell them different things. "All or none" is the
 * whole domain: neither implementation can be told about a subset of the compilation's `.java` sources.
 */
fun VfsBasedProjectEnvironment.javaInterop(
    configuration: CompilerConfiguration,
    withJavaSources: Boolean = true,
): FirJavaInterop =
    if (configuration.useJavaDirect) {
        createJavaDirectJavaInterop(
            if (withJavaSources) configuration.javaSourceRootEntries() else emptyList(),
            // The binary Java classes live as long as the interop, i.e. as long as the compilation which built it.
            BinaryJavaClassCache(binaryClassFileIndex()),
            javaModuleFinder(),
        )
    } else {
        psiJavaInterop(withJavaSources)
    }

private fun CompilerConfiguration.javaSourceRootEntries(): List<JavaSourceRootEntry> =
    getList(CLIConfigurationKeys.CONTENT_ROOTS)
        .filterIsInstance<JavaSourceRoot>()
        .map { root ->
            val prefix = root.packagePrefix?.takeIf { it.isNotEmpty() }?.let(::FqName) ?: FqName.ROOT
            JavaSourceRootEntry(root.file, prefix)
        }

private fun VfsBasedProjectEnvironment.binaryClassFileIndex(): BinaryClassFileIndex {
    val finderFactory = VirtualFileFinderFactory.getInstance(project) as CliVirtualFileFinderFactory
    return finderFactory.binaryClassFileIndex()
}

private fun VfsBasedProjectEnvironment.javaModuleFinder(): JavaModuleFinder {
    val fileManager = project.getService(CoreJavaFileManager::class.java) as? KotlinCliJavaFileManagerImpl
    return fileManager?.javaModuleFinder ?: JavaModuleFinder { null }
}
