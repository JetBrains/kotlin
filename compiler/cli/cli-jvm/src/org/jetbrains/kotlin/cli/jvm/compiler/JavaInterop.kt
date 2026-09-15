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
import org.jetbrains.kotlin.util.PerformanceManager

fun VfsBasedProjectEnvironment.javaInterop(
    configuration: CompilerConfiguration,
    withJavaSources: Boolean = true,
    perfManager: PerformanceManager? = null,
): FirJavaInterop =
    if (configuration.useJavaDirect) {
        createJavaDirectJavaInterop(
            if (withJavaSources) configuration.javaSourceRootEntries() else emptyList(),
            BinaryJavaClassCache(binaryClassFileIndex()),
            javaModuleFinder(),
            perfManager,
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
