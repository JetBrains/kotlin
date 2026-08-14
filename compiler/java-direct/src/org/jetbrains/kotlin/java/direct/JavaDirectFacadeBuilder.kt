/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.asPsiSearchScope
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForSource
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.FqName
import java.util.IdentityHashMap

/**
 * Injects java-direct into FIR JVM sessions via `createJavaFacade`.
 */
fun createJavaDirectJavaFacadeBuilder(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    javaSourcesScope: AbstractProjectFileSearchScope,
): (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade {
    val sourceRootEntries: List<JavaSourceRootEntry> =
        configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS).asSequence()
            .filterIsInstance<JavaSourceRoot>()
            .map { javaRoot ->
                val prefix =
                    if (javaRoot.packagePrefix.isNullOrEmpty()) FqName.ROOT
                    else FqName(javaRoot.packagePrefix!!)
                JavaSourceRootEntry(javaRoot.file, prefix)
            }
            .toList()

    val virtualFileFinderFactory =
        VirtualFileFinderFactory.getInstance(projectEnvironment.project) as? CliVirtualFileFinderFactory

    // Indexed by search scope identity.
    val binaryFinders: MutableMap<AbstractProjectFileSearchScope, JavaClassFinder> = IdentityHashMap()

    return { _, session, moduleData, scope ->
        val finder: JavaClassFinder = when {
            scope === javaSourcesScope -> JavaClassFinderOverAstImpl(session, sourceRootEntries)
            else -> binaryFinders.getOrPut(scope) { binaryClassFinder(virtualFileFinderFactory, scope) }
        }
        FirJavaFacadeForSource(session, moduleData, finder)
    }
}

private fun binaryClassFinder(
    virtualFileFinderFactory: CliVirtualFileFinderFactory?,
    scope: AbstractProjectFileSearchScope,
): JavaClassFinder {
    if (virtualFileFinderFactory == null) return EmptyJavaClassFinder
    val psiSearchScope: GlobalSearchScope = scope.asPsiSearchScope()
    return JavaClassFinderOverBinaryIndex(
        virtualFileFinderFactory.index,
        psiSearchScope,
        virtualFileFinderFactory.enableSearchInCtSym,
    )
}

private object EmptyJavaClassFinder : JavaClassFinder {
    override fun findClass(request: JavaClassFinder.Request): JavaClass? = null

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> = emptyList()

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? = null

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? = null

    override fun canComputeKnownClassNamesInPackage(): Boolean = false
}
