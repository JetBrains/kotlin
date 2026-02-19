/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 * The main idea of this class is for each package to store all roots which contain the package to avoid excessive file system traversal.
 *
 * @param shouldOnlyFindFirstClass The index will stop the search in [findClasses] once it finds one result.
 */
class JvmDependenciesIndexImpl(
    _roots: List<JavaRoot>,
    private val shouldOnlyFindFirstClass: Boolean,
) : JvmDependenciesIndex {
    private val lock = ReentrantLock()

    //these fields are computed based on _roots passed to constructor which are filled in later
    private val roots: List<JavaRoot> by lazy { _roots.toList() }

    private val maxIndex: Int
        get() = roots.size

    // each "Cache" object corresponds to a package
    private class Cache {
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

    // holds the request and the result last time we searched for class
    // helps improve several scenarios, LazyJavaResolverContext.findClassInJava being the most important
    private var lastClassSearch: Pair<ClassSearchRequest, ClassSearchResult>? = null

    override val indexedRoots by lazy { roots.asSequence() }

    private val packageCache: Array<out MutableMap<String, VirtualFile?>> by lazy {
        Array(roots.size) { Object2ObjectOpenHashMap<String, VirtualFile?>() }
    }

    override fun traverseDirectoriesInPackage(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    ) {
        lock.withLock {
            traverseIndex(packageFqName, acceptedRootTypes) { dir, root -> continueSearch(dir, root.type) }
        }
    }

    override fun <T : Any> findClasses(
        classId: ClassId,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        findClassGivenDirectory: (VirtualFile, JavaRoot.RootType) -> T?,
    ): Collection<T> {
        lock.withLock {
            // TODO: KT-58327 probably should be changed to thread local to fix fast-path
            // make a decision based on information saved from last class search
            val cachedClasses = lastClassSearch?.let { (cachedRequest, cachedResult) ->
                if (cachedRequest.classId != classId) return@let null

                when (cachedResult) {
                    is ClassSearchResult.NotFound -> {
                        val limitedRootTypes = acceptedRootTypes - cachedRequest.acceptedRootTypes
                        if (limitedRootTypes.isEmpty()) emptyList() else null
                    }
                    is ClassSearchResult.Found -> {
                        if (cachedRequest.acceptedRootTypes == acceptedRootTypes) {
                            listOfNotNull(findClassGivenDirectory(cachedResult.packageDirectory, cachedResult.root.type))
                        } else null
                    }
                    is ClassSearchResult.FoundMultiple -> {
                        if (cachedRequest.acceptedRootTypes == acceptedRootTypes) {
                            cachedResult.results.mapNotNull { findClassGivenDirectory(it.packageDirectory, it.root.type) }
                        } else null
                    }
                }
            }

            return cachedClasses ?: searchClasses(classId, acceptedRootTypes, findClassGivenDirectory)
        }
    }

    private fun <T : Any> searchClasses(
        classId: ClassId,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        findClassGivenDirectory: (VirtualFile, JavaRoot.RootType) -> T?,
    ): Collection<T> {
        val results = mutableListOf<T>()
        val classSearchResults = mutableListOf<ClassSearchResult.Found>()

        traverseIndex(classId.packageFqName, acceptedRootTypes) { directoryInRoot, root ->
            val result = findClassGivenDirectory(directoryInRoot, root.type)
            if (result != null) {
                results.add(result)
                classSearchResults.add(ClassSearchResult.Found(directoryInRoot, root))

                if (shouldOnlyFindFirstClass) {
                    return@traverseIndex false
                }
            }

            // Traverse the whole index to find all classes. `shouldOnlyFindFirstClass` is handled above.
            true
        }

        val classSearchResult = when (classSearchResults.size) {
            0 -> ClassSearchResult.NotFound
            1 -> classSearchResults.single()
            else -> ClassSearchResult.FoundMultiple(classSearchResults)
        }
        lastClassSearch = ClassSearchRequest(classId, acceptedRootTypes) to classSearchResult

        return results.ifEmpty { emptyList() }
    }

    /**
     * @param handleEntry A function which is given an index entry made up of the directory in the root ([VirtualFile]) and the [JavaRoot].
     *  It should handle this entry, and return whether the traversal should be continued.
     */
    private inline fun traverseIndex(
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
    private fun travelPath(
        rootIndex: Int,
        packageFqName: FqName,
        packagesPath: List<String>,
        fillCachesAfter: Int,
        cachesPath: List<Cache>
    ): VirtualFile? {
        if (rootIndex >= maxIndex) {
            for (i in (fillCachesAfter + 1) until cachesPath.size) {
                // we all know roots that contain this package by now
                cachesPath[i].rootIndices.add(maxIndex)
                cachesPath[i].rootIndices.trimToSize()
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

    private fun cachesPath(path: List<String>): List<Cache> {
        val caches = ArrayList<Cache>(path.size + 1)
        caches.add(rootCache)
        var currentCache = rootCache
        for (subPackageName in path) {
            currentCache = currentCache[subPackageName]
            caches.add(currentCache)
        }
        return caches
    }

    private data class ClassSearchRequest(val classId: ClassId, val acceptedRootTypes: Set<JavaRoot.RootType>)

    private sealed class ClassSearchResult {
        class Found(val packageDirectory: VirtualFile, val root: JavaRoot) : ClassSearchResult()

        class FoundMultiple(val results: List<Found>) : ClassSearchResult()

        data object NotFound : ClassSearchResult()
    }
}
