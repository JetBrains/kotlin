/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.ClassId
import kotlin.concurrent.withLock

/**
 * The compiler implementation of [JvmDependenciesIndex].
 *
 * When searching for a class, the index returns only the first result and does not search for all possible results. This is legal in
 * compiler mode because it has a single-module view.
 */
class JvmDependenciesIndexImpl(roots: List<JavaRoot>) : JvmDependenciesIndexBase(roots) {
    // holds the request and the result last time we searched for class
    // helps improve several scenarios, LazyJavaResolverContext.findClassInJava being the most important
    private var lastClassVirtualFileSearch: Pair<ClassSearchRequest, Collection<VirtualFile>>? = null

    override fun findClassVirtualFiles(
        classId: ClassId,
        acceptedExtensions: JavaFileExtensions,
    ): Collection<VirtualFile> {
        lock.withLock {
            // TODO: KT-58327 probably should be changed to thread local to fix fast-path
            // make a decision based on information saved from last class search
            val cachedClasses = lastClassVirtualFileSearch?.let { (cachedRequest, cachedResult) ->
                if (cachedRequest.classId != classId) return@let null

                val isMatchingRequest = if (cachedResult.isEmpty()) {
                    cachedRequest.acceptedExtensions.containsAll(acceptedExtensions)
                } else {
                    // The accepted extensions have to match exactly. Otherwise, the cache might produce files with unexpected extensions.
                    // The order must also be the same, as it determines which files are resolved first in each root, so the result might be
                    // different.
                    cachedRequest.acceptedExtensions == acceptedExtensions
                }

                if (isMatchingRequest) cachedResult else null
            }

            if (cachedClasses != null) return cachedClasses

            val result = searchClasses(classId, acceptedExtensions)
            lastClassVirtualFileSearch = ClassSearchRequest(classId, acceptedExtensions) to result
            return result
        }
    }

    /**
     * Searches for class virtual files matching the given [classId] and [acceptedExtensions] by traversing the index.
     *
     * This method does not acquire [lock]. The caller is responsible for synchronization.
     */
    private fun searchClasses(
        classId: ClassId,
        acceptedExtensions: JavaFileExtensions,
    ): Collection<VirtualFile> {
        val fileNameWithoutExtension = classId.relativeClassName.asString().replace('.', '$')
        val results = mutableListOf<VirtualFile>()

        traverseIndex(classId.packageFqName, acceptedExtensions.rootTypes) { directoryInRoot, root ->
            for (ext in acceptedExtensions) {
                if (ext.rootType != root.type) continue

                val file = directoryInRoot
                    .findChild("$fileNameWithoutExtension.${ext.extension}")
                    ?.takeIf { it.isValid }

                if (file != null) {
                    results.add(file)

                    // Stop the search. We've found the first class virtual file.
                    return@traverseIndex false
                }
            }

            // Continue the search until a class virtual file has been found.
            true
        }

        return results.ifEmpty { emptyList() }
    }

    private data class ClassSearchRequest(
        val classId: ClassId,
        val acceptedExtensions: JavaFileExtensions,
    )
}
