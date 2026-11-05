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

import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * @param shouldOnlyFindFirstClass The index will stop the search in [findClassFiles] once it finds one result.
 */
class JvmDependenciesDynamicCompoundIndex(private val shouldOnlyFindFirstClass: Boolean) : JvmDependenciesIndex {
    private val indices = arrayListOf<JvmDependenciesIndex>()
    private val lock = ReentrantReadWriteLock()

    @Volatile
    private var currentVersion: Int = 0

    fun addIndex(index: JvmDependenciesIndex) {
        lock.write {
            indices.add(index)
            currentVersion++
        }
    }

    fun getUnindexedRoots(roots: Iterable<JavaRoot>): List<JavaRoot> =
        lock.read {
            val alreadyIndexed = indexedRoots.toHashSet()
            roots.filter { root -> root !in alreadyIndexed }
        }

    override val indexedRoots: Sequence<JavaRoot> get() = indices.asSequence().flatMap { it.indexedRoots }

    override val version: Int get() = currentVersion

    override fun findClassFiles(
        classId: ClassId,
        acceptedExtensions: JavaFileExtensions,
    ): Collection<BinaryClassFileHandle> = lock.read {
        if (shouldOnlyFindFirstClass) {
            listOfNotNull(
                indices.firstNotNullOfOrNull { it.findClassFiles(classId, acceptedExtensions).firstOrNull() }
            )
        } else {
            indices.flatMap { it.findClassFiles(classId, acceptedExtensions) }
        }
    }

    override fun findClassFiles(
        classId: ClassId,
        acceptedExtensions: JavaFileExtensions,
        filter: ClassFileFilter,
    ): Collection<BinaryClassFileHandle> = lock.read {
        if (shouldOnlyFindFirstClass) {
            listOfNotNull(
                indices.firstNotNullOfOrNull { it.findClassFiles(classId, acceptedExtensions, filter).firstOrNull() }
            )
        } else {
            indices.flatMap { it.findClassFiles(classId, acceptedExtensions, filter) }
        }
    }

    override fun traverseFilesInPackage(
        packageFqName: FqName,
        acceptedExtensions: JavaFileExtensions,
        continueSearch: (BinaryClassFileHandle) -> Boolean
    ) = lock.read {
        var stopped = false
        for (index in indices) {
            index.traverseFilesInPackage(packageFqName, acceptedExtensions) { file ->
                continueSearch(file).also { stopped = !it }
            }
            if (stopped) break
        }
    }

    override fun hasPackage(packageFqName: FqName, acceptedRootTypes: Set<JavaRoot.RootType>): Boolean = lock.read {
        indices.any { it.hasPackage(packageFqName, acceptedRootTypes) }
    }

    override fun findBuiltInsFile(packageFqName: FqName): BinaryClassFileHandle? = lock.read {
        indices.firstNotNullOfOrNull { it.findBuiltInsFile(packageFqName) }
    }
}
