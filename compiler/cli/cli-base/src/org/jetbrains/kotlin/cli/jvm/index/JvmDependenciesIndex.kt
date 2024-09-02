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
import java.util.*

interface JvmDependenciesIndex {
    val indexedRoots: Sequence<JavaRoot>

    /**
     * Searches the index for all packages which may contain a class called [classId], and invokes [findClassGivenDirectory] with the
     * [VirtualFile] directory and [JavaRoot] of each package that may contain the class. The function returns the list of classes that were
     * found and created by [findClassGivenDirectory].
     *
     * [findClassGivenDirectory] must check whether the class with this [classId] exists in the given package.
     *
     * [findClasses] may return multiple results for the same [classId] because a [JvmDependenciesIndex] is currently shared between all
     * modules and thus takes a global view on the project. A project may have two libraries which contain a class with the same name. When
     * we have two independent modules which each depend on one library, there is no classpath issue as the libraries do not overlap. In
     * such a case, the global index must provide virtual files for both classes.
     *
     * The [JvmDependenciesIndex] implementation may choose to find only the first result instead of all results if it's operated under a
     * single-module view. This avoids the possible negative performance impact of looking for multiple results.
     */
    fun <T : Any> findClasses(
        classId: ClassId,
        acceptedRootTypes: Set<JavaRoot.RootType> = JavaRoot.SourceAndBinary,
        findClassGivenDirectory: (VirtualFile, JavaRoot.RootType) -> T?
    ): Collection<T>

    fun traverseDirectoriesInPackage(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType> = JavaRoot.SourceAndBinary,
        continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    )
}

data class JavaRoot(val file: VirtualFile, val type: RootType, val prefixFqName: FqName? = null) {
    enum class RootType {
        SOURCE,
        BINARY,
        BINARY_SIG
    }

    companion object RootTypes {
        val OnlyBinary: Set<RootType> = EnumSet.of(RootType.BINARY, RootType.BINARY_SIG)
        val OnlySource: Set<RootType> = EnumSet.of(RootType.SOURCE)
        val SourceAndBinary: Set<RootType> = EnumSet.of(RootType.BINARY, RootType.BINARY_SIG, RootType.SOURCE)
    }
}
