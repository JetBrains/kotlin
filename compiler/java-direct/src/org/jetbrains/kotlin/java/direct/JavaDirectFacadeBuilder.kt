/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileSystem
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
 * For the source scope (Java source roots non-empty) the builder yields a
 * [CombinedJavaClassFinder] over [JavaClassFinderOverAstImpl] + [BinaryJavaClassFinder]; for the
 * library scope (no Java source roots) it yields the binary finder alone. The binary finder is
 * memoised per `(scope identityHash, enableCtSym)` so the source-session and library-session
 * facades share the same binary backing and its caches.
 *
 * This is the temporary solution until we separate source and binary facades.
 */
fun createJavaDirectSourceJavaFacadeBuilder(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
): (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade {
    val localFs = projectEnvironment.knownFileSystems.first { it.protocol == StandardFileSystems.FILE_PROTOCOL }
    val binaryFinderCache: MutableMap<BinaryFinderCacheKey, JavaClassFinder> = HashMap()

    return { _, session, moduleData, scope ->
        val finder = buildJavaDirectClassFinder(
            configuration = configuration,
            localFs = localFs,
            session = session,
            binaryFinderProvider = {
                binaryFinderForScope(projectEnvironment, scope, session, binaryFinderCache)
            },
        ) ?: binaryFinderForScope(projectEnvironment, scope, session, binaryFinderCache)
            ?: throw IllegalStateException("No Java source roots and no binary class finder available for scope $scope")
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

private fun buildJavaDirectClassFinder(
    configuration: CompilerConfiguration,
    localFs: VirtualFileSystem,
    session: FirSession,
    binaryFinderProvider: () -> JavaClassFinder?,
): JavaClassFinder? {
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

    val binaryFinder: JavaClassFinder? = binaryFinderProvider()

    if (sourceRootEntries.isEmpty()) return binaryFinder

    val sourceFinder = JavaClassFinderOverAstImpl(session, sourceRootEntries)
    if (binaryFinder == null) return sourceFinder
    return CombinedJavaClassFinder(sourceFinder, binaryFinder)
}
