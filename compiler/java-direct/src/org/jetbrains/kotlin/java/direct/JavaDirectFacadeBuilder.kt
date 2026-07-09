/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.StandardFileSystems
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
import org.jetbrains.kotlin.fir.java.deserialization.JvmBinaryClassFinderInputs
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Direct-injection seam that plugs `java-direct` into the FIR JVM sessions through the
 * `createJavaFacade` lambda on `FirJvmSessionFactory.createSourceSession` /
 * `createLibrarySession` (populated by `JvmFrontendPipelinePhase.prepareJvmSessions` when
 * `JvmAnalysisFlags.useJavaDirect` is set).
 *
 * The facade dispatches on the search scope:
 *  - **Library scope** (`scope === librariesScope`): a [NoOpJavaClassFinder]-backed facade —
 *    binary classes are read by the deserializer through [JvmBinaryClassFinderInputsOverIndex].
 *  - **Source scope**: a [JavaClassFinderOverAstImpl] over the configured Java source roots
 *    (with no roots the finder is effectively a no-op, so no special-casing is needed).
 *
 * Identity comparison is the correct dispatch key: the scope instances are constructed once in
 * `FirJvmSessionFactory.prepareSessions` and threaded through unchanged.
 */
fun createJavaDirectSourceJavaFacadeBuilder(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    librariesScope: AbstractProjectFileSearchScope,
): (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade {
    val localFs = projectEnvironment.knownFileSystems.first { it.protocol == StandardFileSystems.FILE_PROTOCOL }

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

    return { _, session, moduleData, scope ->
        val finder: JavaClassFinder = when {
            // Library-session facade: deserializer-only, no class lookups through the facade
            // (see file KDoc).
            scope === librariesScope -> NoOpJavaClassFinder
            // Source-session facade: source-only `JavaClassFinderOverAstImpl`. With no Java
            // sources the AST finder is effectively a no-op over an empty index.
            else -> JavaClassFinderOverAstImpl(session, sourceRootEntries)
        }
        FirJavaFacadeForSource(session, moduleData, finder)
    }
}

/**
 * Companion to [createJavaDirectSourceJavaFacadeBuilder]: produces the deserializer-side
 * [JvmBinaryClassFinderInputs] lambda for `FirJvmSessionFactory.createLibrarySession`. On the
 * CLI path it returns a [JvmBinaryClassFinderInputsOverIndex] over the same index
 * `CliVirtualFileFinder` uses, memoised per `(scope identityHash, enableCtSym)`; in non-CLI
 * environments (no `JvmDependenciesIndex`) it returns `null` and the deserializer falls back to
 * `FirJavaFacade`.
 */
@Suppress("UnstableApiUsage")
fun createJavaDirectBinaryClassFinderInputsBuilder(
    projectEnvironment: VfsBasedProjectEnvironment,
): (AbstractProjectEnvironment, AbstractProjectFileSearchScope) -> JvmBinaryClassFinderInputs? {
    val cache: MutableMap<BinaryInputsCacheKey, JvmBinaryClassFinderInputs?> = HashMap()
    return { _, scope ->
        val psiSearchScope: GlobalSearchScope = scope.asPsiSearchScope()
        val vfff = VirtualFileFinderFactory.getInstance(projectEnvironment.project) as? CliVirtualFileFinderFactory
        val key = BinaryInputsCacheKey(System.identityHashCode(psiSearchScope), vfff?.enableSearchInCtSym)
        cache.getOrPut(key) {
            if (vfff != null) {
                JvmBinaryClassFinderInputsOverIndex(vfff.index, psiSearchScope, vfff.enableSearchInCtSym)
            } else {
                null
            }
        }
    }
}

private data class BinaryInputsCacheKey(val scopeIdentity: Int, val enableCtSym: Boolean?)

/**
 * No-op [JavaClassFinder] for the library-session facade. Binary lookups go through the
 * [JvmBinaryClassFinderInputsOverIndex] adapter at the deserializer level, so this finder is
 * consulted only for package annotations (`null` yields an empty list) and for deserializer
 * Elvis fallbacks where `null`/`false` is the correct outcome.
 */
private object NoOpJavaClassFinder : JavaClassFinder {
    override fun findClass(request: JavaClassFinder.Request): JavaClass? = null

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> = emptyList()

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? = null

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? = null

    override fun canComputeKnownClassNamesInPackage(): Boolean = false

    override fun isInSourceIndex(classId: ClassId): Boolean = false

    override fun hasPackageInSources(fqName: FqName): Boolean = false

    override fun sourceClassNamesInPackage(packageFqName: FqName): Set<String>? = null
}
