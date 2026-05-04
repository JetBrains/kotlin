/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.extensions.BinaryJavaClassFinderInputs
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
        binaryClassFinderInputsProvider: (() -> BinaryJavaClassFinderInputs?)?,
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

        // Phase 1 stepping stone (see `implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`):
        // when the system property is on, prefer the index-based finder over the legacy PSI
        // `JavaClassFinderImpl` for the binary half. The provider returns `null` outside the CLI
        // environment, in which case we silently fall back to PSI.
        val binaryFinder: JavaClassFinder? = run {
            if (USE_BINARY_FINDER) {
                binaryClassFinderInputsProvider?.invoke()?.let {
                    BinaryJavaClassFinder(it.index, it.scope, it.enableSearchInCtSym)
                }
            } else null
        } ?: defaultFinderProvider?.invoke()

        // For library session (no Java sources), just use the binary finder we have (if any).
        if (sourceRootEntries.isEmpty()) {
            return binaryFinder
                ?: throw IllegalStateException("No Java source roots and no binary class finder available")
        }

        val sourceFinder = JavaClassFinderOverAstImpl(sourceRootEntries)

        // If no binary finder is available at all, return source-only finder.
        if (binaryFinder == null) return sourceFinder

        // Combine source-based finder (for Java sources) with binary finder (for `.class`/`.sig`).
        return CombinedJavaClassFinder(sourceFinder, binaryFinder)
    }

    private companion object {
        /**
         * Phase 1 feature flag: when set to `true`, the binary half of [CombinedJavaClassFinder]
         * is the new index-based [BinaryJavaClassFinder] instead of the legacy PSI
         * `JavaClassFinderImpl`. Default `false` so the existing 2793/2793 (100%) `JavaUsingAst*`
         * test runs are unaffected; flipping the flag enables the A/B comparison described in
         * §2.6 of `implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`.
         */
        private val USE_BINARY_FINDER: Boolean = true
//            System.getProperty("kotlin.javaDirect.useBinaryClassFinder", "false").toBoolean()
    }
}

private const val PLUGIN_ID = "org.jetbrains.kotlin.javaDirect"
