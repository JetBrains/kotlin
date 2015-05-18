/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.IntArrayList
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.ArrayList
import java.util.EnumSet
import java.util.HashMap
import kotlin.properties.Delegates

public data class JavaRoot(public val file: VirtualFile, public val type: JavaRoot.RootType) {
    public enum class RootType {
        SOURCE,
        BINARY
    }

    companion object RootTypes {
        public val OnlyBinary: Set<RootType> = EnumSet.of(RootType.BINARY)
        public val SourceAndBinary: Set<RootType> = EnumSet.of(RootType.BINARY, RootType.SOURCE)
    }
}

// speeds up finding files/classes in classpath/java source roots
// NOT THREADSAFE, needs to be adapted/removed if we want compiler to be multithreaded
// the main idea of this class is for each package to store roots which contains it to avoid excessive file system traversal
public class JvmDependenciesIndex(_roots: List<JavaRoot>) {

    //these fields are computed based on _roots passed to constructor which are filled in later
    private val roots: List<JavaRoot> by Delegates.lazy { _roots.toList() }

    private val maxIndex: Int
        get() = roots.size()

    // each "Cache" object corresponds to a package
    private class Cache {
        private val innerPackageCaches = HashMap<String, Cache>()

        fun get(name: String) = innerPackageCaches.getOrPut(name) { Cache() }

        // indices of roots that are known to contain this package
        // if this list contains [1, 3, 5] then roots with indices 1, 3 and 5 are known to contain this package, 2 and 4 are known not to (no information about roots 6 or higher)
        // if this list contains maxIndex that means that all roots containing this package are known
        val rootIndices = IntArrayList()
    }

    // root "Cache" object corresponds to DefaultPackage which exists in every root
    private val rootCache: Cache by Delegates.lazy {
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


    // findClassGivenDirectory MUST check whether the class with this classId exists in given package
    public fun <T : Any> findClass(
            classId: ClassId,
            acceptedRootTypes: Set<JavaRoot.RootType> = JavaRoot.SourceAndBinary,
            findClassGivenDirectory: (VirtualFile, JavaRoot.RootType) -> T?
    ): T? {
        return search(FindClassRequest(classId, acceptedRootTypes)) { dir, rootType ->
            val found = findClassGivenDirectory(dir, rootType)
            HandleResult(found, continueSearch = found == null)
        }
    }

    public fun traverseDirectoriesInPackage(
            packageFqName: FqName,
            acceptedRootTypes: Set<JavaRoot.RootType> = JavaRoot.SourceAndBinary,
            continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    ) {
        search(TraverseRequest(packageFqName, acceptedRootTypes)) { dir, rootType ->
            HandleResult(Unit, continueSearch(dir, rootType))
        }
    }

    private data class HandleResult<T : Any>(val result: T?, val continueSearch: Boolean)

    private fun <T : Any> search(
            request: SearchRequest,
            handler: (VirtualFile, JavaRoot.RootType) -> HandleResult<T>
    ): T? {

        // default to searching with given parameters
        fun doSearch() = doSearch(request, handler)

        // make a decision based on information saved from last class search
        if (request !is FindClassRequest || lastClassSearch == null) {
            return doSearch()
        }
        val (cachedRequest, cachedResult) = lastClassSearch!!
        if (cachedRequest.classId != request.classId) {
            return doSearch()
        }
        when (cachedResult) {
            is SearchResult.NotFound -> {
                val limitedRootTypes = request.acceptedRootTypes.toHashSet()
                limitedRootTypes.removeAll(cachedRequest.acceptedRootTypes)
                if (limitedRootTypes.isEmpty()) {
                    return null
                }
                else {
                    return doSearch(FindClassRequest(request.classId, limitedRootTypes), handler)
                }
            }
            is SearchResult.Found -> {
                if (cachedRequest.acceptedRootTypes == request.acceptedRootTypes) {
                    return handler(cachedResult.packageDirectory, cachedResult.root.type).result
                }
            }
        }

        return doSearch()
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
        val packagesPath = request.packageFqName.pathSegments().map { it.getIdentifier() }
        // a list of caches corresponding to packages, [default, "org", "org.jb", "org.jb.kotlin"]
        val caches = cachesPath(packagesPath)

        var processedRootsUpTo = -1
        // traverse caches starting from last, which contains most specific information
        for (cacheIndex in caches.indices.reversed()) {
            val cache = caches[cacheIndex]
            for (i in cache.rootIndices.size().indices) {
                val rootIndex = cache.rootIndices[i]
                if (rootIndex <= processedRootsUpTo) continue // roots with those indices have been processed by now

                val directoryInRoot = travelPath(rootIndex, packagesPath, cacheIndex, caches) ?: continue
                val root = roots[rootIndex]
                val result = handle(root, directoryInRoot)
                if (result != null) {
                    return found(directoryInRoot, root, result)
                }
            }
            processedRootsUpTo = cache.rootIndices.lastOrNull() ?: processedRootsUpTo
        }
        return notFound()
    }

    // try to find a target directory corresponding to package represented by packagesPath in a given root reprenting by index
    // possibly filling "Cache" objects with new information
    private fun travelPath(rootIndex: Int, packagesPath: List<String>, fillCachesAfter: Int, cachesPath: List<Cache>): VirtualFile? {
        if (rootIndex >= maxIndex) {
            for (i in (fillCachesAfter + 1)..cachesPath.size() - 1) {
                // we all know roots that contain this package by now
                cachesPath[i].rootIndices.add(maxIndex)
                cachesPath[i].rootIndices.trimToSize()
            }
            return null
        }

        var currentFile = roots[rootIndex].file
        for (pathIndex in packagesPath.indices) {
            val subPackageName = packagesPath[pathIndex]
            currentFile = currentFile.findChild(subPackageName) ?: return null
            val correspondingCacheIndex = pathIndex + 1
            if (correspondingCacheIndex > fillCachesAfter) {
                // subPackageName exists in this root
                cachesPath[correspondingCacheIndex].rootIndices.add(rootIndex)
            }
        }
        return currentFile
    }

    private fun cachesPath(path: List<String>): List<Cache> {
        val caches = ArrayList<Cache>()
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
            get() = classId.getPackageFqName()
    }

    private data class TraverseRequest(
            override val packageFqName: FqName,
            override val acceptedRootTypes: Set<JavaRoot.RootType>
    ) : SearchRequest

    private trait SearchRequest {
        val packageFqName: FqName
        val acceptedRootTypes: Set<JavaRoot.RootType>
    }

    private trait SearchResult {
        class Found(val packageDirectory: VirtualFile, val root: JavaRoot) : SearchResult

        object NotFound : SearchResult
    }
}

private fun IntArrayList.lastOrNull() = if (isEmpty()) null else get(size() - 1)