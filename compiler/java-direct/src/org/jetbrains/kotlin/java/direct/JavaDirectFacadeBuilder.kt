/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
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
import org.jetbrains.kotlin.name.FqName

/**
 * Direct-injection seam used to plug `java-direct` into the FIR JVM sessions through the
 * `createJavaFacade` lambda parameter on `FirJvmSessionFactory.createSourceSession` and
 * `FirJvmSessionFactory.createLibrarySession`. `JvmFrontendPipelinePhase.prepareJvmSessions`
 * populates the builder when the `JvmAnalysisFlags.useJavaDirect` flag is set.
 */
fun createJavaDirectSourceJavaFacadeBuilder(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
): (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade {
    val localFs = projectEnvironment.knownFileSystems.first { it.protocol == StandardFileSystems.FILE_PROTOCOL }

    return { _, session, moduleData, scope ->
        val finder = buildJavaDirectClassFinder(
            configuration = configuration,
            localFs = localFs,
            session = session,
            binaryFinderProvider = {
                @OptIn(K1Deprecation::class)
                projectEnvironment.project.createJavaClassFinder(scope.asPsiSearchScope(), session.javaAnnotationProvider)
            },
        )
        FirJavaFacadeForSource(session, moduleData, finder)
    }
}

private fun buildJavaDirectClassFinder(
    configuration: CompilerConfiguration,
    localFs: VirtualFileSystem,
    session: FirSession,
    binaryFinderProvider: () -> JavaClassFinder,
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

    val binaryFinder: JavaClassFinder = binaryFinderProvider()

    if (sourceRootEntries.isEmpty()) return binaryFinder

    val sourceFinder = JavaClassFinderOverAstImpl(session, sourceRootEntries)
    return CombinedJavaClassFinder(sourceFinder, binaryFinder)
}
