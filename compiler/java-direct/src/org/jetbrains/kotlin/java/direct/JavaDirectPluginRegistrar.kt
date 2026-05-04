/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.extensions.BinaryJavaClassFinderInputs
import org.jetbrains.kotlin.cli.jvm.compiler.extensions.JavaClassFinderFactory
import org.jetbrains.kotlin.cli.jvm.config.javaSourceRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaAnnotationProvider
import org.jetbrains.kotlin.load.java.JavaClassFinder

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
        // Collect source roots as VirtualFiles so all subsequent reads/walks go through VFS caches.
        val roots: List<VirtualFile> = configuration.javaSourceRoots
            .mapNotNull(localFs::findFileByPath)

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
        if (roots.isEmpty()) {
            return binaryFinder
                ?: throw IllegalStateException("No Java source roots and no binary class finder available")
        }

        val sourceFinder = JavaClassFinderOverAstImpl(roots)

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
        private val USE_BINARY_FINDER: Boolean =
            System.getProperty("kotlin.javaDirect.useBinaryClassFinder", "false").toBoolean()
    }
}

private const val PLUGIN_ID = "org.jetbrains.kotlin.javaDirect"
