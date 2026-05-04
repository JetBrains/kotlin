/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.extensions.JavaClassFinderFactory
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaAnnotationProvider
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.name.FqName

class JavaDirectPluginRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        JavaClassFinderFactory.registerExtension(JavaClassFinderOverAstFactory(configuration))
    }

    override val pluginId: String get() = PLUGIN_ID

    override val supportsK2: Boolean
        get() = true
}

class JavaClassFinderOverAstFactory(private val configuration: CompilerConfiguration) : JavaClassFinderFactory {
    override fun createJavaClassFinder(
        scope: AbstractProjectFileSearchScope,
        annotationProvider: JavaAnnotationProvider?,
        localFs: VirtualFileSystem,
        defaultFinderProvider: (() -> JavaClassFinder)?,
    ): JavaClassFinder {
        val sourceRootEntries: List<JavaSourceRootEntry> =
            configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS).asSequence()
                .filterIsInstance<JavaSourceRoot>()
                .mapNotNull { javaRoot ->
                    val vFile = localFs.findFileByPath(javaRoot.file.path) ?: return@mapNotNull null
                    val prefix =
                        if (javaRoot.packagePrefix.isNullOrEmpty()) FqName.ROOT
                        else FqName(javaRoot.packagePrefix!!)
                    JavaSourceRootEntry(vFile, prefix)
                }
                .toList()

        // For library session (no Java sources), just use the default finder
        if (sourceRootEntries.isEmpty()) {
            return defaultFinderProvider?.invoke()
                ?: throw IllegalStateException("No Java source roots and no default finder provider")
        }

        val sourceFinder = JavaClassFinderOverAstImpl(sourceRootEntries)

        val binaryFinder = defaultFinderProvider?.invoke() ?: return sourceFinder

        return CombinedJavaClassFinder(sourceFinder, binaryFinder)
    }
}

private const val PLUGIN_ID = "org.jetbrains.kotlin.javaDirect"
