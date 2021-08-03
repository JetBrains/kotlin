/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaElementFinder
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import java.io.File

class PsiBasedProjectFileSearchScope(val psiSearchScope: GlobalSearchScope) : AbstractProjectFileSearchScope {

    override val isEmpty: Boolean
        get() = psiSearchScope == GlobalSearchScope.EMPTY_SCOPE

    override operator fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(psiSearchScope.intersectWith(GlobalSearchScope.notScope(other.asPsiSearchScope())))

    override operator fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(psiSearchScope.uniteWith(other.asPsiSearchScope()))

    override operator fun not(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(GlobalSearchScope.notScope(psiSearchScope))
}

class PsiBasedProjectEnvironment(
    val project: Project,
    val localFileSystem: VirtualFileSystem,
    val getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider
) : AbstractProjectEnvironment {

    override fun getKotlinClassFinder(fileSearchScope: AbstractProjectFileSearchScope): KotlinClassFinder =
        VirtualFileFinderFactory.getInstance(project).create(fileSearchScope.asPsiSearchScope())

    override fun getJavaClassFinder(fileSearchScope: AbstractProjectFileSearchScope): JavaClassFinder =
        JavaClassFinderImpl().apply {
            this.setProjectInstance(project)
            this.setScope(fileSearchScope.asPsiSearchScope())
        }

    override fun getJavaSymbolProvider(
        firSession: FirSession,
        baseModuleData: FirModuleData,
        fileSearchScope: AbstractProjectFileSearchScope
    ): JavaSymbolProvider =
        JavaSymbolProvider(firSession, baseModuleData, project, fileSearchScope.asPsiSearchScope())

    override fun getPackagePartProvider(fileSearchScope: AbstractProjectFileSearchScope): PackagePartProvider =
        getPackagePartProviderFn(fileSearchScope.asPsiSearchScope())

    override fun registerAsJavaElementFinder(firSession: FirSession) {
        val psiFinderExtensionPoint = PsiElementFinder.EP.getPoint(project)
        if (psiFinderExtensionPoint.extensionList.any { it is JavaElementFinder }) {
            psiFinderExtensionPoint.unregisterExtension(JavaElementFinder::class.java)
        }
        psiFinderExtensionPoint.registerExtension(FirJavaElementFinder(firSession, project), project)
    }

    override fun getSearchScopeByIoFiles(files: Iterable<File>, allowOutOfProjectRoots: Boolean): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            files
                .mapNotNull { localFileSystem.findFileByPath(it.absolutePath) }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.let {
                    if (allowOutOfProjectRoots) GlobalSearchScope.filesWithLibrariesScope(project, it)
                    else GlobalSearchScope.filesWithoutLibrariesScope(project, it)
                } ?: GlobalSearchScope.EMPTY_SCOPE
        )

    fun getSearchScopeByPsiFiles(files: Iterable<PsiFile>, allowOutOfProjectRoots: Boolean= false): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            files.map { it.virtualFile }.let {
                if (allowOutOfProjectRoots) GlobalSearchScope.filesWithLibrariesScope(project, it)
                else GlobalSearchScope.filesWithoutLibrariesScope(project, it)
            }
        )

    override fun getSearchScopeForProjectLibraries(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))

    override fun getSearchScopeForProjectJavaSources(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))

}

private fun AbstractProjectFileSearchScope.asPsiSearchScope() =
    when {
        this === AbstractProjectFileSearchScope.EMPTY -> GlobalSearchScope.EMPTY_SCOPE
        this === AbstractProjectFileSearchScope.ANY -> GlobalSearchScope.notScope(GlobalSearchScope.EMPTY_SCOPE)
        else -> (this as PsiBasedProjectFileSearchScope).psiSearchScope
    }

fun KotlinCoreEnvironment.toAbstractProjectEnvironment(): AbstractProjectEnvironment =
    PsiBasedProjectEnvironment(
        project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
        { createPackagePartProvider(it) }
    )

fun GlobalSearchScope.toAbstractProjectFileSearchScope(): AbstractProjectFileSearchScope =
    PsiBasedProjectFileSearchScope(this)
