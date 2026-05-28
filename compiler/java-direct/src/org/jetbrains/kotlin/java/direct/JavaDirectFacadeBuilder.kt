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
import org.jetbrains.kotlin.fir.java.javaAnnotationProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.FqName

/**
 * Direct-injection seam used to plug `java-direct` into the FIR JVM sessions through
 * `FirJvmSessionFactory.Context.javaFacadeBuilder`. The production CLI populates the builder in
 * `JvmFrontendPipelinePhase.preprocessSessions`; test fixtures populate the equivalent
 * `JavaFacadeBuilderProvider` `TestService` via `JavaDirectFacadeBuilderProvider`.
 *
 * Stage 2 §6.4 of [`compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`]
 * (partial — source-side only; the library-side `BinaryJavaClassFinder` lambda stays until §6.3
 * / §6.5 absorb its body into `JvmClassFileBasedSymbolProvider`):
 *
 *  - **Source scope** (the lambda call whose `scope` is *not* identity-equal to [librariesScope]):
 *    `FirJavaFacadeForSource(session, moduleData, JavaClassFinderOverAstImpl(session, roots))` —
 *    source-only, no [CombinedJavaClassFinder] involvement. Safe because §6.2 narrowed
 *    `JavaSymbolProvider` to source-only probes and the source facade's `findClass` is only
 *    reached from `JavaSymbolProvider.classCache.createValue` for `classId`s already gated by
 *    `isInSourceIndex`.
 *  - **Library scope** (`scope === librariesScope`): [BinaryJavaClassFinder] alone — unchanged
 *    behaviour vs the pre-§6.4 wiring for that branch. The binary finder is memoised per
 *    `(scope identityHash, enableCtSym)` exactly as before.
 *  - **Source roots empty** (pure-Kotlin compile / no Java sources at all): the source-scope
 *    branch falls through to the binary finder too, matching today's behaviour (a source session
 *    without Java sources still needs to resolve binary Java classes via its facade, e.g. for
 *    the `convertJavaClassToFir`-driven legacy paths that haven't yet migrated to the
 *    deserializer-owned binary path).
 *
 * Identity comparison `scope === librariesScope` is the correct dispatch key because
 * [AbstractProjectFileSearchScope] instances are created in `FirJvmSessionFactory.prepareSessions`
 * and threaded through unchanged; the two scopes (`javaSourcesScope`, `librariesScope`) are
 * distinct objects with disjoint identity.
 */
fun createJavaDirectSourceJavaFacadeBuilder(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    librariesScope: AbstractProjectFileSearchScope,
): (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade {
    val localFs = projectEnvironment.knownFileSystems.first { it.protocol == StandardFileSystems.FILE_PROTOCOL }
    val binaryFinderCache: MutableMap<BinaryFinderCacheKey, JavaClassFinder> = HashMap()

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
            // Library-session facade: binary finder only. The deserializer
            // (`JvmClassFileBasedSymbolProvider`) reads through this facade until §6.3 / §6.5
            // move its body inline.
            scope === librariesScope -> {
                binaryFinderForScope(projectEnvironment, scope, session, binaryFinderCache)
                    ?: throw IllegalStateException("No binary class finder available for library scope $scope")
            }
            // Source-session facade with Java sources: source-only `JavaClassFinderOverAstImpl`.
            // Binary references in source signatures are resolved via the symbol provider chain
            // (deserializer / `JavaSymbolProvider` etc.), not through this facade.
            sourceRootEntries.isNotEmpty() -> JavaClassFinderOverAstImpl(session, sourceRootEntries)
            // Source-session facade in a pure-Kotlin compile (no Java sources at all): fall
            // through to the binary finder for the same scope, matching today's behaviour for
            // source sessions that have nothing to read from `JavaClassFinderOverAstImpl`.
            else -> binaryFinderForScope(projectEnvironment, scope, session, binaryFinderCache)
                ?: throw IllegalStateException("No Java source roots and no binary class finder available for scope $scope")
        }
        FirJavaFacadeForSource(session, moduleData, finder)
    }
}

private data class BinaryFinderCacheKey(val scopeIdentity: Int, val enableCtSym: Boolean?)

@Suppress("UnstableApiUsage")
private fun binaryFinderForScope(
    projectEnvironment: VfsBasedProjectEnvironment,
    scope: AbstractProjectFileSearchScope,
    session: FirSession,
    cache: MutableMap<BinaryFinderCacheKey, JavaClassFinder>,
): JavaClassFinder? {
    val psiSearchScope: GlobalSearchScope = scope.asPsiSearchScope()
    val vfff = VirtualFileFinderFactory.getInstance(projectEnvironment.project) as? CliVirtualFileFinderFactory
    val key = BinaryFinderCacheKey(System.identityHashCode(psiSearchScope), vfff?.enableSearchInCtSym)
    cache[key]?.let { return it }
    val finder: JavaClassFinder =
        if (vfff != null) {
            BinaryJavaClassFinder(vfff.index, psiSearchScope, vfff.enableSearchInCtSym)
        } else {
            // Non-CLI environment without a `JvmDependenciesIndex`: fall back to PSI for the
            // binary half so the source facade still resolves binary references.
            projectEnvironment.project.createJavaClassFinder(psiSearchScope, session.javaAnnotationProvider)
        }
    cache[key] = finder
    return finder
}
