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

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * @param shouldOnlyFindFirstClass The index will stop the search in [findClassVirtualFiles] once it finds one result.
 */
class JvmDependenciesDynamicCompoundIndex(private val shouldOnlyFindFirstClass: Boolean) : JvmDependenciesIndex {
    private val indices = arrayListOf<JvmDependenciesIndex>()
    private val lock = ReentrantReadWriteLock()

    fun addIndex(index: JvmDependenciesIndex) {
        lock.write {
            indices.add(index)
        }
    }

    fun getUnindexedRoots(roots: Iterable<JavaRoot>): List<JavaRoot> =
        lock.read {
            val alreadyIndexed = indexedRoots.toHashSet()
            roots.filter { root -> root !in alreadyIndexed }
        }

    override val indexedRoots: Sequence<JavaRoot> get() = indices.asSequence().flatMap { it.indexedRoots }

    override fun findClassVirtualFiles(
        classId: ClassId,
        acceptedExtensions: JavaFileExtensions,
    ): Collection<VirtualFile> = lock.read {
        if (shouldOnlyFindFirstClass) {
            listOfNotNull(
                indices.firstNotNullOfOrNull { it.findClassVirtualFiles(classId, acceptedExtensions).firstOrNull() }
            )
        } else {
            indices.flatMap { it.findClassVirtualFiles(classId, acceptedExtensions) }
        }
    }

    override fun traverseClassVirtualFilesInPackage(
        packageFqName: FqName,
        acceptedExtensions: JavaFileExtensions,
        continueSearch: (VirtualFile) -> Boolean
    ) = lock.read {
        indices.forEach { it.traverseClassVirtualFilesInPackage(packageFqName, acceptedExtensions, continueSearch) }
    }

    override fun traverseDirectoriesInPackage(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    ) = lock.read {
        indices.forEach { it.traverseDirectoriesInPackage(packageFqName, acceptedRootTypes, continueSearch) }
    }

    override fun traverseVirtualFilesInPackage(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    ) = lock.read {
        indices.forEach { it.traverseVirtualFilesInPackage(packageFqName, acceptedRootTypes, continueSearch) }
    }
}
