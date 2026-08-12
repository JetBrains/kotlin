/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.java.FirJavaElementFinder
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForModule
import org.jetbrains.kotlin.fir.java.javaAnnotationProvider
import org.jetbrains.kotlin.fir.session.FirJavaInterop
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmCompilationEnvironment
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

open class VfsBasedProjectEnvironment(
    val project: Project,
    val knownFileSystems: List<VirtualFileSystem>,
    private val getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider
) : JvmCompilationEnvironment {

    constructor(
        project: Project,
        fileSystem: VirtualFileSystem,
        getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider
    ) : this(project, listOf(fileSystem), getPackagePartProviderFn)

    override fun getKotlinClassFinder(classpath: JvmClasspath): KotlinClassFinder =
        VirtualFileFinderFactory.getInstance(project).create(psiSearchScope(classpath))

    override fun getJavaModuleResolver(): JavaModuleResolver =
        JavaModuleResolver.getInstance(project)

    override fun getPackagePartProvider(classpath: JvmClasspath): PackagePartProvider =
        getPackagePartProviderFn(psiSearchScope(classpath))

    /**
     * A (maybe temporary) mechanism for extending the classpath handled by package providers; nop by default
     */
    open fun updateClasspath(classpath: List<File>) {}

    /**
     * The only place where a [JvmClasspath] becomes an IntelliJ scope: the PSI infrastructure indexes all binaries
     * at once and restricts a lookup afterwards, so every view this environment hands out is index + scope.
     */
    fun psiSearchScope(classpath: JvmClasspath): GlobalSearchScope = when (classpath) {
        is JvmClasspath.Roots -> classPathScope(classpath.roots)
        is JvmClasspath.ProjectLibraries -> {
            val libraries = ProjectScope.getLibrariesScope(project)
            when {
                classpath.excludedRoots.isEmpty() -> libraries
                else -> libraries.intersectWith(GlobalSearchScope.notScope(classPathScope(classpath.excludedRoots)))
            }
        }
        is PsiScopeJvmClasspath -> classpath.psiSearchScope
        else -> error("Unexpected ${JvmClasspath::class.simpleName}: $classpath")
    }

    private fun classPathScope(roots: List<Path>): GlobalSearchScope =
        roots
            .mapNotNull {
                // this code is somewhat ad hoc, but currently it is exactly the logic of classpath processing that we're using in
                // the cli compiler
                when {
                    it.isDirectory() -> knownFileSystems.findFileByPath(it.toFile().absolutePath, StandardFileSystems.FILE_PROTOCOL)
                    !it.isRegularFile() -> null
                    else -> knownFileSystems.findFileByPath(it.toFile().absolutePath + JAR_SEPARATOR, StandardFileSystems.JAR_PROTOCOL)
                }
            }
            .takeIf { it.isNotEmpty() }
            ?.let { ClassPathScope(project, it) }
            ?: GlobalSearchScope.EMPTY_SCOPE

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

}

/**
 * The PSI-based Java view: the `JavaClassFinder` reads the `.java` sources and the `.class` files through PSI,
 * and the Kotlin declarations of a source session are registered back as PSI stubs so that this Java resolution
 * can see them. The peer of `createJavaDirectJavaInterop` in `:compiler:java-direct`; an alternative Java
 * implementation replaces not the facade but the [org.jetbrains.kotlin.load.java.JavaClassFinder] inside it.
 *
 * [javaSources] is what this compilation calls the `.java` sources of a module: all of them by default,
 * none for scripting and the REPL, the module's own files in the multi-module test infrastructure.
 */
fun VfsBasedProjectEnvironment.psiJavaInterop(
    javaSources: (FirModuleData) -> GlobalSearchScope = { AllJavaSourcesInProjectScope(project) },
): FirJavaInterop = object : FirJavaInterop {
    override fun createBinaryJavaFacade(
        session: FirSession,
        moduleData: FirModuleData,
        classpath: JvmClasspath,
    ): FirJavaFacade = createJavaFacade(session, moduleData, psiSearchScope(classpath))

    override fun createJavaSourcesFacade(
        session: FirSession,
        moduleData: FirModuleData,
    ): FirJavaFacade = createJavaFacade(session, moduleData, javaSources(moduleData))

    private fun createJavaFacade(session: FirSession, moduleData: FirModuleData, scope: GlobalSearchScope): FirJavaFacade {
        val javaClassFinder = project.createJavaClassFinder(scope, session.javaAnnotationProvider)
        return FirJavaFacadeForModule(session, moduleData, javaClassFinder)
    }

    @OptIn(SessionConfiguration::class)
    override fun registerKotlinDeclarationsForJava(session: FirSession) {
        val psiFinderExtensionPoint = PsiElementFinder.EP.getPoint(project)
        psiFinderExtensionPoint.unregisterFinders<FirJavaElementFinder>()

        val firJavaElementFinder = FirJavaElementFinder(session, project)
        session.register(FirJavaElementFinder::class, firJavaElementFinder)
        // see comment and TODO in KotlinCoreEnvironment.registerKotlinLightClassSupport (KT-64296)
        @Suppress("DEPRECATION")
        PsiElementFinder.EP.getPoint(project).registerExtension(firJavaElementFinder)
        Disposer.register(project) {
            psiFinderExtensionPoint.unregisterFinders<FirJavaElementFinder>()
        }
    }
}


fun KotlinCoreEnvironment.toVfsBasedProjectEnvironment(): VfsBasedProjectEnvironment =
    VfsBasedProjectEnvironment(
        project,
        listOfNotNull(
            projectEnvironment.jarFileSystem,
            projectEnvironment.environment.jrtFileSystem,
            projectEnvironment.environment.localFileSystem,
        ),
    ) { createPackagePartProvider(it) }

/**
 * An escape hatch for code which still describes a part of the classpath as an IntelliJ scope instead of as
 * roots: the legacy JKlib IR pipeline and some test fixtures. It is understood by
 * [VfsBasedProjectEnvironment.psiSearchScope] and by nothing else, so it must not leave `:compiler:cli`.
 */
class PsiScopeJvmClasspath(val psiSearchScope: GlobalSearchScope) : JvmClasspath

fun GlobalSearchScope.asJvmClasspath(): JvmClasspath = PsiScopeJvmClasspath(this)

inline fun <reified T : PsiElementFinder> ExtensionPoint<PsiElementFinder>.unregisterFinders() {
    if (extensionList.any { it is T }) {
        unregisterExtension(T::class.java)
    }
}

internal fun List<VirtualFileSystem>.findFileByPath(
    path: String,
    protocolFilter: String? = StandardFileSystems.FILE_PROTOCOL
): VirtualFile? =
    firstNotNullOfOrNull {
        if (protocolFilter != null && it.protocol != protocolFilter) null
        else it.findFileByPath(path)
    }

fun VfsBasedProjectEnvironment.findFileByPath(path: String): VirtualFile? = knownFileSystems.findFileByPath(path)
