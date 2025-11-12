/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.java.FirJavaElementFinder
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForSource
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory

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

open class VfsBasedProjectEnvironment(
    val project: Project,
    val knownFileSystems: List<VirtualFileSystem>,
    private val getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider
) : AbstractProjectEnvironment {

    constructor(project: Project, fileSystem: VirtualFileSystem, getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider) :
            this(project, listOf(fileSystem), getPackagePartProviderFn)

    override fun getKotlinClassFinder(fileSearchScope: AbstractProjectFileSearchScope): KotlinClassFinder =
        VirtualFileFinderFactory.getInstance(project).create(fileSearchScope.asPsiSearchScope())

    override fun getJavaModuleResolver(): JavaModuleResolver =
        JavaModuleResolver.getInstance(project)

    override fun getPackagePartProvider(fileSearchScope: AbstractProjectFileSearchScope): PackagePartProvider =
        getPackagePartProviderFn(fileSearchScope.asPsiSearchScope())

    @OptIn(SessionConfiguration::class)
    override fun registerAsJavaElementFinder(firSession: FirSession) {
        val psiFinderExtensionPoint = PsiElementFinder.EP.getPoint(project)
        psiFinderExtensionPoint.unregisterFinders<JavaElementFinder>()
        psiFinderExtensionPoint.unregisterFinders<FirJavaElementFinder>()

        val firJavaElementFinder = FirJavaElementFinder(firSession, project)
        firSession.register(FirJavaElementFinder::class, firJavaElementFinder)
        // see comment and TODO in KotlinCoreEnvironment.registerKotlinLightClassSupport (KT-64296)
        @Suppress("DEPRECATION")
        PsiElementFinder.EP.getPoint(project).registerExtension(firJavaElementFinder)
        Disposer.register(project) {
            psiFinderExtensionPoint.unregisterFinders<FirJavaElementFinder>()
        }
    }

    private fun List<VirtualFile>.toSearchScope(allowOutOfProjectRoots: Boolean) =
        takeIf { it.isNotEmpty() }
            ?.let {
                if (allowOutOfProjectRoots) GlobalSearchScope.filesWithLibrariesScope(project, it)
                else GlobalSearchScope.filesWithoutLibrariesScope(project, it)
            }
            ?: GlobalSearchScope.EMPTY_SCOPE

    override fun getSearchScopeByIoFiles(files: Iterable<File>, allowOutOfProjectRoots: Boolean): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            files
                .mapNotNull { file -> knownFileSystems.findFileByPath(file.absolutePath) }
                .toSearchScope(allowOutOfProjectRoots)
        )

    override fun getSearchScopeBySourceFiles(files: Iterable<KtSourceFile>, allowOutOfProjectRoots: Boolean): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            files
                .mapNotNull {
                    when (it) {
                        is KtPsiSourceFile -> it.psiFile.virtualFile
                        is KtVirtualFileSourceFile -> it.virtualFile
                        is KtIoFileSourceFile -> knownFileSystems.findFileByPath(it.file.absolutePath)
                        else -> null // TODO: find out whether other use cases should be supported
                    }
                }
                .toSearchScope(allowOutOfProjectRoots)
        )

    override fun getSearchScopeByDirectories(directories: Iterable<File>): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            directories
                .mapNotNull { knownFileSystems.findFileByPath(it.absolutePath) }
                .toSet()
                .takeIf { it.isNotEmpty() }
                ?.let {
                    KotlinToJVMBytecodeCompiler.DirectoriesScope(project, it)
                } ?: GlobalSearchScope.EMPTY_SCOPE
        )

    override fun getSearchScopeByClassPath(paths: Iterable<Path>): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            paths
                .mapNotNull {
                    // this code is somewhat ad hoc, but currently it is exactly the logic of classpath processing that we're using in
                    // the cli compiler
                    if (it.isDirectory()) knownFileSystems.findFileByPath(it.toFile().absolutePath, StandardFileSystems.FILE_PROTOCOL)
                    else knownFileSystems.findFileByPath(it.toFile().absolutePath + JAR_SEPARATOR, StandardFileSystems.JAR_PROTOCOL)
                }
                .takeIf { it.isNotEmpty() }
                ?.let {
                    ClassPathScope(project, it)
                } ?: GlobalSearchScope.EMPTY_SCOPE
        )

    private class ClassPathScope(
        project: Project,
        roots: Iterable<VirtualFile>, // matching relies on the correct VirtualFile.filesystem for all roots
    ) : DelegatingGlobalSearchScope(allScope(project)) {
        private val fileSystemsToRoots = HashMap<VirtualFileSystem, HashSet<VirtualFile>>()

        init {
            // NB: groupBy(To) cannot be used, because it hardcodes List as the value
            for (root in roots) {
                val fs = root.fileSystem
                fileSystemsToRoots.getOrPut(fs) { HashSet() }.add(root)
            }
        }

        override fun contains(file: VirtualFile): Boolean {
            val possibleRoots = fileSystemsToRoots[file.fileSystem] ?: return false
            val prefixPos = file.path.indexOf(JAR_SEPARATOR)
            if (prefixPos >= 0) {
                // jar/jrt fs, prefix should match
                val root = file.fileSystem.findFileByPath(file.path.substring(0, prefixPos + JAR_SEPARATOR.length))
                return root in possibleRoots
            }
            // else subdir search (same as in [KotlinToJVMBytecodeCompiler.DirectoriesScope]
            var parent: VirtualFile = file
            while (true) {
                if (parent in possibleRoots) return true
                parent = parent.parent ?: return false
            }
        }

        override fun toString() = "All files under: ${fileSystemsToRoots.values.flatten().joinToString { it.path}}"
    }

    fun getSearchScopeByPsiFiles(files: Iterable<PsiFile>): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            files.map { it.virtualFile }.let {
                GlobalSearchScope.filesWithoutLibrariesScope(project, it)
            }
        )

    override fun getSearchScopeForProjectLibraries(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))

    override fun getSearchScopeForProjectJavaSources(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))

    override fun getFirJavaFacade(
        firSession: FirSession,
        baseModuleData: FirModuleData,
        fileSearchScope: AbstractProjectFileSearchScope
    ) = FirJavaFacadeForSource(firSession, baseModuleData, project.createJavaClassFinder(fileSearchScope.asPsiSearchScope()))

}

private fun AbstractProjectFileSearchScope.asPsiSearchScope() =
    when {
        this === AbstractProjectFileSearchScope.EMPTY -> GlobalSearchScope.EMPTY_SCOPE
        this === AbstractProjectFileSearchScope.ANY -> GlobalSearchScope.notScope(GlobalSearchScope.EMPTY_SCOPE)
        else -> (this as PsiBasedProjectFileSearchScope).psiSearchScope
    }

fun KotlinCoreEnvironment.toVfsBasedProjectEnvironment(): VfsBasedProjectEnvironment =
    VfsBasedProjectEnvironment(
        project,
        listOfNotNull(
            projectEnvironment.jarFileSystem,
            projectEnvironment.environment.jrtFileSystem,
            projectEnvironment.environment.localFileSystem,
        ),
        { createPackagePartProvider(it) }
    )

fun GlobalSearchScope.toAbstractProjectFileSearchScope(): AbstractProjectFileSearchScope =
    PsiBasedProjectFileSearchScope(this)

inline fun <reified T : PsiElementFinder> ExtensionPoint<PsiElementFinder>.unregisterFinders() {
    if (extensionList.any { it is T }) {
        unregisterExtension(T::class.java)
    }
}

private fun List<VirtualFileSystem>.findFileByPath(
    path: String,
    protocolFilter: String? = StandardFileSystems.FILE_PROTOCOL
): VirtualFile? =
    firstNotNullOfOrNull {
        if (protocolFilter != null && it.protocol != protocolFilter) null
        else it.findFileByPath(path)
    }
