/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.IntArrayList
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.*

// speeds up finding files/classes in classpath/java source roots
// NOT THREADSAFE, needs to be adapted/removed if we want compiler to be multithreaded
// the main idea of this class is for each package to store roots which contains it to avoid excessive file system traversal
class JvmDependenciesIndexImpl(_roots: List<JavaRoot>): JvmDependenciesIndex {
    //these fields are computed based on _roots passed to constructor which are filled in later
    private val roots: List<JavaRoot> by lazy { _roots.toList() }

    private val maxIndex: Int
        get() = roots.size

    // each "Cache" object corresponds to a package
    private class Cache {
        private val innerPackageCaches = HashMap<String, Cache>()

        operator fun get(name: String) = innerPackageCaches.getOrPut(name) { Cache() }

        // indices of roots that are known to contain this package
        // if this list contains [1, 3, 5] then roots with indices 1, 3 and 5 are known to contain this package, 2 and 4 are known not to (no information about roots 6 or higher)
        // if this list contains maxIndex that means that all roots containing this package are known
        val rootIndices = IntArrayList()
    }

    // root "Cache" object corresponds to DefaultPackage which exists in every root. Roots with non-default fqname are also listed here but
    // they will be ignored on requests with invalid fqname prefix.
    private val rootCache: Cache by lazy {
        with(Cache()) {
            roots.indices.forEach {
                rootIndices.add(it)
            }
            rootIndices.add(maxIndex)
            rootIndices.trimToSize()
            this
        }
    }

    // holds the request and the result last time we searched for class
    // helps improve several scenarios, LazyJavaResolverContext.findClassInJava being the most important
    private var lastClassSearch: Pair<FindClassRequest, SearchResult>? = null

    override val indexedRoots by lazy { roots.asSequence() }

    // findClassGivenDirectory MUST check whether the class with this classId exists in given package
    override fun <T : Any> findClass(
            classId: ClassId,
            acceptedRootTypes: Set<JavaRoot.RootType>,
            findClassGivenDirectory: (VirtualFile, JavaRoot.RootType) -> T?
    ): T? {
        return search(FindClassRequest(classId, acceptedRootTypes)) { dir, rootType ->
            val found = findClassGivenDirectory(dir, rootType)
            HandleResult(found, continueSearch = found == null)
        }
    }

    override fun traverseDirectoriesInPackage(
            packageFqName: FqName,
            acceptedRootTypes: Set<JavaRoot.RootType>,
            continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    ) {
        search(TraverseRequest(packageFqName, acceptedRootTypes)) { dir, rootType ->
            HandleResult(Unit, continueSearch(dir, rootType))
        }
    }

    override fun collectKnownClassNamesInPackage(
            packageFqName: FqName
    ): Set<String> {
        val result = hashSetOf<String>()
        traverseDirectoriesInPackage(packageFqName, continueSearch = {
            dir, rootType ->

            for (child in dir.children) {
                if (child.extension != "class" && child.extension != "java") continue
                result.add(child.nameWithoutExtension)
            }

            true
        })

        return result
    }

    private data class HandleResult<out T : Any>(val result: T?, val continueSearch: Boolean)

    private fun <T : Any> search(
            request: SearchRequest,
            handler: (VirtualFile, JavaRoot.RootType) -> HandleResult<T>
    ): T? {
        // make a decision based on information saved from last class search
        if (request !is FindClassRequest || lastClassSearch?.first?.classId != request.classId) {
            return doSearch(request, handler)
        }
        
        val (cachedRequest, cachedResult) = lastClassSearch!!
        return when (cachedResult) {
            is SearchResult.NotFound -> {
                val limitedRootTypes = request.acceptedRootTypes.toHashSet()
                limitedRootTypes.removeAll(cachedRequest.acceptedRootTypes)
                if (limitedRootTypes.isEmpty()) {
                    null
                }
                else {
                    doSearch(FindClassRequest(request.classId, limitedRootTypes), handler)
                }
            }
            is SearchResult.Found -> {
                if (cachedRequest.acceptedRootTypes == request.acceptedRootTypes) {
                    handler(cachedResult.packageDirectory, cachedResult.root.type).result
                }
                else {
                    doSearch(request, handler)
                }
            }
        }
    }

    private fun <T : Any> doSearch(request: SearchRequest, handler: (VirtualFile, JavaRoot.RootType) -> HandleResult<T>): T? {
        val findClassRequest = request as? FindClassRequest

        fun <T : Any> found(packageDirectory: VirtualFile, root: JavaRoot, result: T): T {
            if (findClassRequest != null) {
                lastClassSearch = Pair(findClassRequest, SearchResult.Found(packageDirectory, root))
            }
            return result
        }

        fun <T : Any> notFound(): T? {
            if (findClassRequest != null) {
                lastClassSearch = Pair(findClassRequest, SearchResult.NotFound)
            }
            return null
        }

        fun handle(root: JavaRoot, targetDirInRoot: VirtualFile): T? {
            if (root.type in request.acceptedRootTypes) {
                val (result, shouldContinue) = handler(targetDirInRoot, root.type)
                if (!shouldContinue) {
                    return result
                }
            }
            return null
        }

        // a list of package sub names, ["org", "jb", "kotlin"]
        val packagesPath = request.packageFqName.pathSegments().map { it.identifier }
        // a list of caches corresponding to packages, [default, "org", "org.jb", "org.jb.kotlin"]
        val caches = cachesPath(packagesPath)

        var processedRootsUpTo = -1
        // traverse caches starting from last, which contains most specific information

        // NOTE: indices manipulation instead of using caches.indices.reversed() is here for performance reasons
        val cachesLastIndex = caches.lastIndex
        for (cacheIndex in 0..cachesLastIndex) {
            val reverseCacheIndex = cachesLastIndex - cacheIndex
            val cacheRootIndices = caches[reverseCacheIndex].rootIndices
            for (i in 0..cacheRootIndices.size() - 1) {
                val rootIndex = cacheRootIndices[i]
                if (rootIndex <= processedRootsUpTo) continue // roots with those indices have been processed by now

                val directoryInRoot = travelPath(rootIndex, packagesPath, reverseCacheIndex, caches) ?: continue
                val root = roots[rootIndex]
                val result = handle(root, directoryInRoot)
                if (result != null) {
                    return found(directoryInRoot, root, result)
                }
            }
            processedRootsUpTo = if (cacheRootIndices.isEmpty) processedRootsUpTo else cacheRootIndices.get(cacheRootIndices.size() - 1)
        }
        
        return notFound()
    }

    // try to find a target directory corresponding to package represented by packagesPath in a given root reprenting by index
    // possibly filling "Cache" objects with new information
    private fun travelPath(rootIndex: Int, packagesPath: List<String>, fillCachesAfter: Int, cachesPath: List<Cache>): VirtualFile? {
        if (rootIndex >= maxIndex) {
            for (i in (fillCachesAfter + 1)..(cachesPath.size - 1)) {
                // we all know roots that contain this package by now
                cachesPath[i].rootIndices.add(maxIndex)
                cachesPath[i].rootIndices.trimToSize()
            }
            return null
        }

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
            }
            else {
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

    private data class FindClassRequest(val classId: ClassId, override val acceptedRootTypes: Set<JavaRoot.RootType>) : SearchRequest {
        override val packageFqName: FqName
            get() = classId.packageFqName
    }

    private data class TraverseRequest(
            override val packageFqName: FqName,
            override val acceptedRootTypes: Set<JavaRoot.RootType>
    ) : SearchRequest

    private interface SearchRequest {
        val packageFqName: FqName
        val acceptedRootTypes: Set<JavaRoot.RootType>
    }

    private sealed class SearchResult {
        class Found(val packageDirectory: VirtualFile, val root: JavaRoot) : SearchResult()

        object NotFound : SearchResult()
    }
}
