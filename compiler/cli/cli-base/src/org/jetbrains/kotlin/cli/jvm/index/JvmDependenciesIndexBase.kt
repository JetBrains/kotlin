/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// TODO: KT-58327 needs to be adapted/removed if we want compiler to be multithreaded

/**
 * Speeds up finding files/classes in the classpath/Java source roots.
 *
 * The main idea of this class is for each package to store all roots that contain the package to avoid excessive file system traversal.
 */
abstract class JvmDependenciesIndexBase(protected val roots: List<JavaRoot>) : JvmDependenciesIndex {
    protected val lock = ReentrantLock()

    private val maxIndex: Int
        get() = roots.size

    // each "Cache" object corresponds to a package
    protected class Cache {
        private val innerPackageCaches = HashMap<String, Cache>()

        operator fun get(name: String) = innerPackageCaches.getOrPut(name, ::Cache)

        // indices of roots that are known to contain this package
        // if this list contains [1, 3, 5] then roots with indices 1, 3 and 5 are known to contain this package, 2 and 4 are known not to (no information about roots 6 or higher)
        // if this list contains maxIndex that means that all roots containing this package are known
        @Suppress("DEPRECATION") // TODO: fix deprecation
        val rootIndices = com.intellij.util.containers.IntArrayList(2)
    }

    // root "Cache" object corresponds to DefaultPackage which exists in every root. Roots with non-default fqname are also listed here but
    // they will be ignored on requests with invalid fqname prefix.
    private val rootCache: Cache by lazy {
        Cache().apply {
            roots.indices.forEach(rootIndices::add)
            rootIndices.add(maxIndex)
            rootIndices.trimToSize()
        }
    }

    override val indexedRoots by lazy { roots.asSequence() }

    private val packageCache: Array<out MutableMap<String, VirtualFile?>> by lazy {
        Array(roots.size) { Object2ObjectOpenHashMap<String, VirtualFile?>() }
    }

    final override fun traverseDirectoriesInPackage(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    ) {
        lock.withLock {
            traverseIndex(packageFqName, acceptedRootTypes) { dir, root -> continueSearch(dir, root.type) }
        }
    }

    final override fun traverseVirtualFilesInPackage(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    ) {
        lock.withLock {
            traverseIndex(packageFqName, acceptedRootTypes) { dir, root ->
                for (child in dir.children) {
                    val shouldContinueSearch = continueSearch(child, root.type)
                    if (!shouldContinueSearch) return@traverseIndex false
                }
                true
            }
        }
    }

    abstract override fun findClassVirtualFiles(
        classId: ClassId,
        acceptedExtensions: JavaFileExtensions,
    ): Collection<VirtualFile>

    override fun traverseClassVirtualFilesInPackage(
        packageFqName: FqName,
        acceptedExtensions: JavaFileExtensions,
        continueSearch: (VirtualFile) -> Boolean,
    ) {
        traverseVirtualFilesInPackage(packageFqName, acceptedExtensions.rootTypes) { file, _ ->
            val extension = file.extension ?: return@traverseVirtualFilesInPackage true

            if (extension in acceptedExtensions) {
                continueSearch(file)
            } else {
                true
            }
        }
    }

    /**
     * @param handleEntry A function that is given an index entry made up of the directory in the root ([VirtualFile]) and the [JavaRoot].
     *  It should handle this entry and return whether the traversal should be continued.
     */
    protected inline fun traverseIndex(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        handleEntry: (VirtualFile, JavaRoot) -> Boolean,
    ) {
        // a list of package sub names, ["org", "jb", "kotlin"]
        val packagesPath = packageFqName.pathSegments().map { it.identifierOrNullIfSpecial ?: return }
        // a list of caches corresponding to packages, [default, "org", "org.jb", "org.jb.kotlin"]
        val caches = cachesPath(packagesPath)

        var processedRootsUpTo = -1
        // traverse caches starting from last, which contains most specific information

        // NOTE: indices manipulation instead of using caches.reversed() is here for performance reasons
        for (cacheIndex in caches.lastIndex downTo 0) {
            val cacheRootIndices = caches[cacheIndex].rootIndices
            for (i in 0 until cacheRootIndices.size()) {
                val rootIndex = cacheRootIndices[i]
                if (rootIndex <= processedRootsUpTo) continue // roots with those indices have been processed by now

                val directoryInRoot = travelPath(rootIndex, packageFqName, packagesPath, cacheIndex, caches) ?: continue
                val root = roots[rootIndex]
                if (root.type in acceptedRootTypes) {
                    val continueTraversal = handleEntry(directoryInRoot, root)
                    if (!continueTraversal) {
                        return
                    }
                }
            }
            processedRootsUpTo = if (cacheRootIndices.isEmpty) processedRootsUpTo else cacheRootIndices[cacheRootIndices.size() - 1]
        }
    }

    // try to find a target directory corresponding to package represented by packagesPath in a given root represented by index
    // possibly filling "Cache" objects with new information
    protected fun travelPath(
        rootIndex: Int,
        packageFqName: FqName,
        packagesPath: List<String>,
        fillCachesAfter: Int,
        cachesPath: List<Cache>
    ): VirtualFile? {
        if (rootIndex >= maxIndex) {
            for (i in (fillCachesAfter + 1) until cachesPath.size) {
                // We know all roots that contain this package by now.
                cachesPath[i].rootIndices.apply {
                    add(maxIndex)
                    trimToSize()
                }
            }
            return null
        }

        return packageCache[rootIndex].getOrPut(packageFqName.asString()) {
            doTravelPath(rootIndex, packagesPath, fillCachesAfter, cachesPath)
        }
    }

    private fun doTravelPath(rootIndex: Int, packagesPath: List<String>, fillCachesAfter: Int, cachesPath: List<Cache>): VirtualFile? {
        val pathRoot = roots[rootIndex]
        val prefixPathSegments = pathRoot.prefixFqName?.pathSegments()

        var currentFile = pathRoot.file

        for (pathIndex in packagesPath.indices) {
            val subPackageName = packagesPath[pathIndex]
            if (prefixPathSegments != null && pathIndex < prefixPathSegments.size) {
                // Traverse prefix first instead of traversing real directories
                if (prefixPathSegments[pathIndex].identifier != subPackageName) {
                    return null
                }
            } else {
                currentFile = currentFile.findChildPackage(subPackageName, pathRoot.type) ?: return null
            }

            val correspondingCacheIndex = pathIndex + 1
            if (correspondingCacheIndex > fillCachesAfter) {
                // subPackageName exists in this root
                cachesPath[correspondingCacheIndex].rootIndices.add(rootIndex)
            }
        }

        return currentFile
    }

    private fun VirtualFile.findChildPackage(subPackageName: String, rootType: JavaRoot.RootType): VirtualFile? {
        val childDirectory = findChild(subPackageName) ?: return null

        val fileExtension = when (rootType) {
            JavaRoot.RootType.BINARY -> JavaClassFileType.INSTANCE.defaultExtension
            JavaRoot.RootType.BINARY_SIG -> "sig"
            JavaRoot.RootType.SOURCE -> JavaFileType.INSTANCE.defaultExtension
        }

        // If in addition to a directory "foo" there's a class file "foo.class" AND there are no classes anywhere in the directory "foo",
        // then we ignore the directory and let the resolution choose the class "foo" instead.
        if (findChild("$subPackageName.$fileExtension")?.isDirectory == false) {
            if (VfsUtilCore.processFilesRecursively(childDirectory) { file -> file.extension != fileExtension }) {
                return null
            }
        }

        return childDirectory
    }

    protected fun cachesPath(path: List<String>): List<Cache> {
        val caches = ArrayList<Cache>(path.size + 1)
        caches.add(rootCache)
        var currentCache = rootCache
        for (subPackageName in path) {
            currentCache = currentCache[subPackageName]
            caches.add(currentCache)
        }
        return caches
    }
}
