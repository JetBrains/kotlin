/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.caches.project.cacheInvalidatingOnRootModifications
import org.jetbrains.kotlin.idea.configuration.IdeBuiltInsLoadingState
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.vfilefinder.KotlinStdlibIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// TODO(kirpichenkov): works only for JVM (see KT-44552)
interface KotlinStdlibCache {
    fun isStdlib(libraryInfo: LibraryInfo): Boolean
    fun isStdlibDependency(libraryInfo: LibraryInfo): Boolean
    fun findStdlibInModuleDependencies(module: IdeaModuleInfo): LibraryInfo?

    companion object {
        fun getInstance(project: Project): KotlinStdlibCache =
            if (IdeBuiltInsLoadingState.isFromClassLoader) {
                Disabled
            } else {
                ServiceManager.getService(project, KotlinStdlibCache::class.java)
                    ?: error("Failed to load service ${KotlinStdlibCache::class.java.name}")
            }

        val Disabled = object : KotlinStdlibCache {
            override fun isStdlib(libraryInfo: LibraryInfo) = false
            override fun isStdlibDependency(libraryInfo: LibraryInfo) = false
            override fun findStdlibInModuleDependencies(module: IdeaModuleInfo): LibraryInfo? = null
        }
    }
}

class KotlinStdlibCacheImpl(val project: Project) : KotlinStdlibCache {
    @JvmInline
    private value class StdlibDependency(val libraryInfo: LibraryInfo?)

    private val isStdlibCache: MutableMap<LibraryInfo, Boolean>
        get() = project.cacheInvalidatingOnRootModifications {
            ConcurrentHashMap<LibraryInfo, Boolean>()
        }

    private val isStdlibDependencyCache: MutableMap<LibraryInfo, Boolean>
        get() = project.cacheInvalidatingOnRootModifications {
            ConcurrentHashMap<LibraryInfo, Boolean>()
        }

    private val moduleStdlibDependencyCache: MutableMap<IdeaModuleInfo, StdlibDependency>
        get() = project.cacheInvalidatingOnRootModifications {
            ConcurrentHashMap<IdeaModuleInfo, StdlibDependency>()
        }

    private class LibraryScope(
        project: Project,
        private val directories: Set<VirtualFile>
    ) : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
        private val fileSystems = directories.mapTo(hashSetOf(), VirtualFile::getFileSystem)

        override fun contains(file: VirtualFile): Boolean =
            file.fileSystem in fileSystems && generateSequence(file, VirtualFile::getParent).any { it in directories }

        override fun toString() = "All files under: $directories"
    }

    private fun libraryScopeContainsIndexedFilesForNames(libraryInfo: LibraryInfo, names: Collection<FqName>) = runReadAction {
        names.any { name ->
            FileBasedIndex.getInstance().getContainingFiles(
                KotlinStdlibIndex.KEY,
                name,
                LibraryScope(project, libraryInfo.library.rootProvider.getFiles(OrderRootType.CLASSES).toSet())
            ).isNotEmpty()
        }
    }

    private fun libraryScopeContainsIndexedFilesForName(libraryInfo: LibraryInfo, name: FqName) =
        libraryScopeContainsIndexedFilesForNames(libraryInfo, listOf(name))

    override fun isStdlib(libraryInfo: LibraryInfo): Boolean {
        return isStdlibCache.getOrPut(libraryInfo) {
            libraryScopeContainsIndexedFilesForName(libraryInfo, KotlinStdlibIndex.KOTLIN_STDLIB_NAME)
        }
    }

    override fun isStdlibDependency(libraryInfo: LibraryInfo): Boolean {
        return isStdlibDependencyCache.getOrPut(libraryInfo) {
            libraryScopeContainsIndexedFilesForNames(libraryInfo, KotlinStdlibIndex.STANDARD_LIBRARY_DEPENDENCY_NAMES)
        }
    }

    override fun findStdlibInModuleDependencies(module: IdeaModuleInfo): LibraryInfo? {
        val stdlibDependency = moduleStdlibDependencyCache.getOrPut(module) {

            fun IdeaModuleInfo.asStdLibInfo() = this.safeAs<LibraryInfo>()?.takeIf { isStdlib(it) }

            val stdLib: LibraryInfo? = module.asStdLibInfo() ?: run {
                val checkedLibraryInfo = mutableSetOf<IdeaModuleInfo>()
                val stack = ArrayDeque<IdeaModuleInfo>()
                stack.add(module)

                // bfs
                while (stack.isNotEmpty()) {
                    ProgressManager.checkCanceled()

                    val poll = stack.poll()
                    if (!checkedLibraryInfo.add(poll)) continue

                    stack += poll.dependencies().also { dependencies ->
                        dependencies
                            .firstOrNull { it is LibraryInfo && isStdlib(it) }
                            ?.let { return@run it as LibraryInfo }
                    }
                }
                null
            }
            StdlibDependency(stdLib)
        }

        return stdlibDependency.libraryInfo
    }
}

fun LibraryInfo.isCoreKotlinLibrary(project: Project): Boolean =
    isKotlinStdlib(project) || isKotlinStdlibDependency(project)

fun LibraryInfo.isKotlinStdlib(project: Project): Boolean =
    KotlinStdlibCache.getInstance(project).isStdlib(this)

fun LibraryInfo.isKotlinStdlibDependency(project: Project): Boolean =
    KotlinStdlibCache.getInstance(project).isStdlibDependency(this)
