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
 * `FirJvmSessionFactory.createLibrarySession`. The production CLI populates the builder in
 * `JvmFrontendPipelinePhase.prepareJvmSessions`; test fixtures populate the equivalent
 * `JavaFacadeBuilderProvider` `TestService` via `JavaDirectFacadeBuilderProvider`.
 *
 * Stage 2 §6.4 + §6.5 of [`compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`]
 * — final shape after `CombinedJavaClassFinder` and `BinaryJavaClassFinder` were deleted:
 *
 *  - **Library scope** (`scope === librariesScope`): a [NoOpJavaClassFinder]-backed
 *    [FirJavaFacadeForSource]. The deserializer ([JvmClassFileBasedSymbolProvider]) reads
 *    binary `.class`/`.sig` files through the deserializer-side
 *    [JvmBinaryClassFinderInputsOverIndex] adapter (built by
 *    [createJavaDirectBinaryClassFinderInputsBuilder]); the only remaining consumer of this
 *    library facade is `JvmClassFileBasedSymbolProvider.extractClassMetadata` →
 *    `javaFacade.convertJavaClassToFir(...)`, which reads only the resolved [JavaClass] and the
 *    cached `javaPackage` annotations. A null `javaPackage` (returned by [NoOpJavaClassFinder])
 *    is observationally identical to today's empty `BinaryJavaPackage.annotations = emptyList()`
 *    on the java-direct path — the chain `javaPackage?.annotations?.mapNotNull { it.classId }`
 *    yields the same effective `emptyList()` either way for downstream consumers
 *    ([FirAnnotationTypeQualifierResolver], [FirMustUseReturnValueStatusComponent]).
 *  - **Source scope** (any non-library scope): a [JavaClassFinderOverAstImpl] backed by the
 *    configured Java source roots. After §6.2 + §6.5, [JavaClassFinderOverAstImpl] overrides
 *    `isInSourceIndex` to delegate to `isClassInIndex`, so `JavaSymbolProvider`'s source-only
 *    gate works as before — binary classes flow through the deserializer.
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
            // Library-session facade: deserializer-only, no class lookups through the facade.
            // `convertJavaClassToFir` is the sole consumer and doesn't touch the finder beyond
            // a cached `findPackage`, for which `null` is equivalent to today's empty
            // `BinaryJavaPackage` — see file KDoc.
            scope === librariesScope -> NoOpJavaClassFinder
            // Source-session facade: source-only `JavaClassFinderOverAstImpl`. When the project
            // has no Java sources at all, the AST finder is effectively a no-op (empty index),
            // which is exactly the desired behaviour for a pure-Kotlin compile's source facade.
            else -> JavaClassFinderOverAstImpl(session, sourceRootEntries)
        }
        FirJavaFacadeForSource(session, moduleData, finder)
    }
}

/**
 * Stage 2 §6.3 + §6.5 of [`compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`]
 * — companion to [createJavaDirectSourceJavaFacadeBuilder]. Produces the deserializer-side
 * [JvmBinaryClassFinderInputs] lambda passed to
 * [org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.createLibrarySession] as
 * `createBinaryClassFinderInputs`. After §6.5, this builder is the **sole owner** of the
 * `java-direct` binary-side construction — the deleted `BinaryJavaClassFinder` is no longer
 * instantiated anywhere else.
 *
 * For the CLI java-direct path (where `CliVirtualFileFinderFactory` is available) the lambda
 * returns a [JvmBinaryClassFinderInputsOverIndex] backed by the same [JvmDependenciesIndex]
 * `CliVirtualFileFinder` uses, memoised per `(scope identityHash, enableCtSym)`. The
 * deserializer ([org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider])
 * then reads binary `.class` (and optionally `.sig`) files directly through this adapter
 * instead of routing through `FirJavaFacade`.
 *
 * For non-CLI environments without a `JvmDependenciesIndex`, the lambda returns `null` and
 * the deserializer falls back to `FirJavaFacade` — semantically equivalent to today's
 * pre-§6.3 behaviour on that path.
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
            // continues to route through `FirJavaFacade` exactly as before §6.3.
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
 * No-op [JavaClassFinder] used for the `java-direct` **library-session** facade after §6.5
 * removed `BinaryJavaClassFinder` / `CombinedJavaClassFinder`.
 *
 * The library facade's [JavaClassFinder] is consulted by exactly two paths on the java-direct
 * library session:
 *
 *  1. [org.jetbrains.kotlin.fir.java.FirJavaFacade]'s internal `packageCache` (read once per
 *     class by `convertJavaClassToFir` via `javaPackage = packageCache.getValue(...)`).
 *     Returning `null` from [findPackage] is observationally identical to the pre-§6.5
 *     `BinaryJavaPackage(fqName)` (which carried `annotations = emptyList()`): downstream
 *     consumers ([FirAnnotationTypeQualifierResolver], [FirMustUseReturnValueStatusComponent])
 *     normalise both cases through `javaPackage?.annotations?.orEmpty()` chains.
 *  2. [org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider]'s
 *     `findBinaryClass` / `hasTopLevelBinaryClass` Elvis fallbacks
 *     (`binaryClassFinderInputs?.X(...) ?: javaFacade.X(...)`). On the java-direct CLI path the
 *     adapter is always non-null, but the Elvis Elvis-`null`s through when the adapter itself
 *     returns `null` (e.g. when a binary class file carries `@Metadata` and is therefore
 *     filtered out — Kotlin-classes are handled by the Kotlin metadata branch of
 *     `extractClassMetadata` instead). The pre-§6.5 fallback to `FirJavaFacade.findClass`
 *     applied the same `@Metadata` filter (`klass.isFromSource || !klass.hasMetadataAnnotation()`)
 *     on top of `BinaryJavaClassFinder.findClass` and also returned `null`; this no-op
 *     preserves that behaviour by returning `null` silently.
 *
 * All other binary lookups (`findClass`/`findPackage`/`knownClassNamesInPackage`/etc.) are
 * routed through the [JvmBinaryClassFinderInputsOverIndex] adapter at the deserializer level
 * (see [createJavaDirectBinaryClassFinderInputsBuilder]), so the corresponding `JavaClassFinder`
 * methods on this no-op are *not* expected to influence resolution — they only matter when the
 * deserializer's Elvis fallback runs, and in every such case the pre-§6.5 chain returned the
 * same `null`/`false`/empty values that this no-op returns.
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
