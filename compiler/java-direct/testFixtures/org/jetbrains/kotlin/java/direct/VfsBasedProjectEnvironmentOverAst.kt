/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForSource
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import java.nio.file.Path

/**
 * A [VfsBasedProjectEnvironment] that uses [JavaClassFinderOverAstImpl] for Java sources in the module
 * (i.e., for non-library search scopes), while delegating library-related lookups to the default implementation.
 */
class VfsBasedProjectEnvironmentOverAst(
    project: Project,
    fileSystem: VirtualFileSystem,
    private val getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider,
    private val librariesScope: AbstractProjectFileSearchScope,
    private val javaSourceRoots: List<Path>,
) : VfsBasedProjectEnvironment(project, fileSystem, getPackagePartProviderFn) {

    override fun getFirJavaFacade(
        firSession: FirSession,
        baseModuleData: FirModuleData,
        fileSearchScope: AbstractProjectFileSearchScope,
    ): FirJavaFacadeForSource {
        // For libraries we keep the default behavior (PSI/bytecode-based lookup).
        if (fileSearchScope === librariesScope) {
            return super.getFirJavaFacade(firSession, baseModuleData, fileSearchScope)
        }

        val javaClassFinder = JavaClassFinderOverAstImpl(javaSourceRoots)
        return FirJavaFacadeForSource(firSession, baseModuleData, javaClassFinder)
    }
}