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
 * @param shouldOnlyFindFirstClass The index will stop the search in [findClasses] once it finds one result.
 */
class JvmDependenciesDynamicCompoundIndex(private val shouldOnlyFindFirstClass: Boolean) : JvmDependenciesIndex {
    private val indices = arrayListOf<JvmDependenciesIndex>()
    private val lock = ReentrantReadWriteLock()

    fun addIndex(index: JvmDependenciesIndex) {
        lock.write {
            indices.add(index)
        }
    }

    fun addNewIndexForRoots(roots: Iterable<JavaRoot>): JvmDependenciesIndex? =
        lock.read {
            val alreadyIndexed = indexedRoots.toHashSet()
            val newRoots = roots.filter { root -> root !in alreadyIndexed }
            if (newRoots.isEmpty()) null
            else JvmDependenciesIndexImpl(newRoots, shouldOnlyFindFirstClass).also(this::addIndex)
        }

    override val indexedRoots: Sequence<JavaRoot> get() = indices.asSequence().flatMap { it.indexedRoots }

    override fun <T : Any> findClasses(
        classId: ClassId,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        findClassGivenDirectory: (VirtualFile, JavaRoot.RootType) -> T?,
    ): Collection<T> = lock.read {
        if (shouldOnlyFindFirstClass) {
            listOfNotNull(
                indices.asSequence()
                    .mapNotNull { it.findClasses(classId, acceptedRootTypes, findClassGivenDirectory).firstOrNull() }
                    .firstOrNull()
            )
        } else {
            indices.flatMap { it.findClasses(classId, acceptedRootTypes, findClassGivenDirectory) }
        }
    }

    override fun traverseDirectoriesInPackage(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType>,
        continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    ) = lock.read {
        indices.forEach { it.traverseDirectoriesInPackage(packageFqName, acceptedRootTypes, continueSearch) }
    }
}
