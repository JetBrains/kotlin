/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.asJvmClasspathRootId
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents

/*
 * This interface is used by custom IC components implementation in IntelliJ.
 */
@Deprecated("Mark the previous build's output with VirtualJvmClasspathRoot.isPrecompiledOutput instead")
interface IncrementalCompilationComponentsWithCustomScope : IncrementalCompilationComponents {
    @Suppress("DEPRECATION")
    fun createSearchScope(projectEnvironment: VfsBasedProjectEnvironment):
            org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
}

/**
 * Converting previous build's output privided via [IncrementalCompilationComponentsWithCustomScope]
 * to the classpath representation.
 */
internal fun IncrementalCompilationComponents.precompiledOutputRootsFromCustomScope(
    projectEnvironment: VfsBasedProjectEnvironment,
): List<JvmClasspathRootId>? {
    @Suppress("DEPRECATION")
    if (this !is IncrementalCompilationComponentsWithCustomScope) return null
    val roots = createSearchScope(projectEnvironment).directories()
    projectEnvironment.registerIndexedClasspathRoots(roots)
    return roots.map { it.asJvmClasspathRootId() }
}

@Suppress("DEPRECATION")
private fun org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope.directories(): Set<VirtualFile> {
    @Suppress("DEPRECATION")
    val psiScope = (this as? org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope)?.psiSearchScope
    return (psiScope as? VfsBasedProjectEnvironment.DirectoriesScope)?.directories
        ?: error(
            "An incremental compilation scope has to be a PsiBasedProjectFileSearchScope over " +
                    "VfsBasedProjectEnvironment.DirectoriesScope, got: $this"
        )
}
