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
 * Direct-injection seam used to plug `java-direct` into the FIR JVM sessions through the
 * `createJavaFacade` lambda parameter on `FirJvmSessionFactory.createSourceSession` and
 * `FirJvmSessionFactory.createLibrarySession`. `JvmFrontendPipelinePhase.prepareJvmSessions`
 * populates the builder when the `JvmAnalysisFlags.useJavaDirect` flag is set.
 *
 * The facade dispatches on the search scope:
 *
 *  - **Library scope** (`scope === librariesScope`): a [NoOpJavaClassFinder]-backed
 *    [FirJavaFacadeForSource]. The deserializer ([JvmClassFileBasedSymbolProvider]) reads
 *    binary `.class`/`.sig` files through the deserializer-side
 *    [JvmBinaryClassFinderInputsOverIndex] adapter (built by
 *    [createJavaDirectBinaryClassFinderInputsBuilder]); the only consumer of this library
 *    facade is `JvmClassFileBasedSymbolProvider.extractClassMetadata` →
 *    `javaFacade.convertJavaClassToFir(...)`, which reads only the resolved [JavaClass] and the
 *    cached `javaPackage` annotations. A null `javaPackage` (returned by [NoOpJavaClassFinder])
 *    yields the same effective empty list of package annotations for downstream consumers
 *    ([FirAnnotationTypeQualifierResolver], [FirMustUseReturnValueStatusComponent]).
 *  - **Source scope** (any non-library scope): a [JavaClassFinderOverAstImpl] backed by the
 *    configured Java source roots. It overrides `isInSourceIndex` to delegate to
 *    `isClassInIndex`, so `JavaSymbolProvider`'s source-only gate works correctly — binary
 *    classes flow through the deserializer.
 *  - **No Java source roots** (pure-Kotlin compile): the source facade is also backed by a
 *    [JavaClassFinderOverAstImpl] over the (empty) source-root list. A finder over an empty
 *    index is effectively a no-op — `findClass`/`findPackage` always return `null` — so this
 *    is functionally identical to the library branch and needs no special-casing.
 *
 * Identity comparison `scope === librariesScope` is the correct dispatch key because
 * [AbstractProjectFileSearchScope] instances are constructed once in
 * `FirJvmSessionFactory.prepareSessions` (or the test-fixture equivalent) and threaded through
 * unchanged; `javaSourcesScope` and `librariesScope` are distinct objects with disjoint
 * identity.
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
 * Companion to [createJavaDirectSourceJavaFacadeBuilder]. Produces the deserializer-side
 * [JvmBinaryClassFinderInputs] lambda passed to
 * [org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.createLibrarySession] as
 * `createBinaryClassFinderInputs`.
 *
 * For the CLI java-direct path (where `CliVirtualFileFinderFactory` is available) the lambda
 * returns a [JvmBinaryClassFinderInputsOverIndex] backed by the same [JvmDependenciesIndex]
 * `CliVirtualFileFinder` uses, memoised per `(scope identityHash, enableCtSym)`. The
 * deserializer ([org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider])
 * then reads binary `.class` (and optionally `.sig`) files directly through this adapter
 * instead of routing through `FirJavaFacade`.
 *
 * For non-CLI environments without a `JvmDependenciesIndex`, the lambda returns `null` and
 * the deserializer falls back to `FirJavaFacade`.
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
            // Only the CLI environment has a `JvmDependenciesIndex`. PSI-based non-CLI
            // environments (scripting, REPL, IC outside CLI) don't, so the deserializer
            // routes through `FirJavaFacade` instead.
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
 * No-op [JavaClassFinder] for the `java-direct` **library-session** facade. Binary lookups go
 * through the [JvmBinaryClassFinderInputsOverIndex] adapter at the deserializer level (see
 * [createJavaDirectBinaryClassFinderInputsBuilder]), so the facade's finder is consulted only on
 * two paths, both of which are satisfied by returning empty/`null`:
 *
 *  1. [org.jetbrains.kotlin.fir.java.FirJavaFacade]'s `packageCache`, read once per class by
 *     `convertJavaClassToFir`. A `null` [findPackage] yields an empty package-annotation list
 *     for downstream consumers ([FirAnnotationTypeQualifierResolver],
 *     [FirMustUseReturnValueStatusComponent]).
 *  2. [org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider]'s
 *     `findBinaryClass` / `hasTopLevelBinaryClass` Elvis fallbacks, which only run when the
 *     adapter returns `null` (e.g. a binary class carrying `@Metadata`, handled by the Kotlin
 *     metadata branch instead). Returning `null`/`false` here is the correct outcome.
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
