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
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.JvmCompilationEnvironment
import org.jetbrains.kotlin.jvm.environment.asJvmClasspathRootId
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.io.File
import java.nio.file.InvalidPathException
import kotlin.io.path.Path
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

    private val indexedClasspathRoots = HashMap<JvmClasspathRootId, VirtualFile>()

    /**
     * Records the classpath roots of this compilation, so that a [JvmClasspathRootId] can be resolved among them
     * instead of being looked for in a file system. Every root is already handed to the project by the same loops
     * (`KotlinCoreEnvironment.updateClasspathFromRootsIndex` and its counterpart in `JvmFrontendPipelinePhase`),
     * so this costs nothing — and it is what lets a build system name a root of its own file system, which no
     * amount of probing could find.
     */
    fun registerIndexedClasspathRoots(roots: Iterable<VirtualFile>) {
        for (root in roots) {
            indexedClasspathRoots[root.asJvmClasspathRootId()] = root
        }
    }

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
    }

    private fun classPathScope(roots: List<JvmClasspathRootId>): GlobalSearchScope =
        roots
            .mapNotNull { indexedClasspathRoots[it] ?: findRootInFileSystems(it) }
            .takeIf { it.isNotEmpty() }
            ?.let { ClassPathScope(project, it) }
            ?: GlobalSearchScope.EMPTY_SCOPE

    /**
     * The fallback for a root which was not registered as indexed — a test fixture naming a root directly, or a
     * classpath computed after the index was built. It is somewhat ad hoc, but currently it is exactly the logic
     * of classpath processing that we're using in the cli compiler.
     */
    private fun findRootInFileSystems(root: JvmClasspathRootId): VirtualFile? {
        val path = try {
            Path(root.id)
        } catch (_: InvalidPathException) {
            // The root is not a location in any file system, so it can only be one of the registered ones.
            return null
        }
        return when {
            path.isDirectory() -> knownFileSystems.findFileByPath(root.id, StandardFileSystems.FILE_PROTOCOL)
            !path.isRegularFile() -> null
            else -> knownFileSystems.findFileByPath(root.id + JAR_SEPARATOR, StandardFileSystems.JAR_PROTOCOL)
        }
    }

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
 * [withJavaSources] says whether this compilation has `.java` sources of its own at all: it has them by
 * default, and it has none in scripting and the REPL. There is nothing in between, because the Kotlin half
 * of a narrower scope is unobservable — [org.jetbrains.kotlin.load.JavaClassFinderImpl] wraps whatever scope
 * it is given in `FilterOutKotlinSourceFilesScope` — and the Java half is always all `.java` files.
 */
fun VfsBasedProjectEnvironment.psiJavaInterop(
    withJavaSources: Boolean = true,
): FirJavaInterop = object : FirJavaInterop {
    override fun createBinaryJavaFacade(
        session: FirSession,
        moduleData: FirModuleData,
        classpath: JvmClasspath,
    ): FirJavaFacade = createJavaFacade(session, moduleData, psiSearchScope(classpath))

    override fun createJavaSourcesFacade(
        session: FirSession,
        moduleData: FirModuleData,
    ): FirJavaFacade = createJavaFacade(
        session, moduleData,
        if (withJavaSources) AllJavaSourcesInProjectScope(project) else GlobalSearchScope.EMPTY_SCOPE,
    )

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
    ) { createPackagePartProvider(it) }.also {
        it.registerIndexedClasspathRoots(indexedClasspathRoots)
    }


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
