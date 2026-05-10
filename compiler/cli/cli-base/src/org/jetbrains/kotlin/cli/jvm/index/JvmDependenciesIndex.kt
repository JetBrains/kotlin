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
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import java.util.*

interface JvmDependenciesIndex {
    val indexedRoots: Sequence<JavaRoot>

    /**
     * Returns [VirtualFile]s that correspond to a class called [classId] in the index.
     *
     * For each package directory in the index, the implementation derives the relative class name from [classId] and looks up files
     * matching each extension in [acceptedExtensions]. Only extensions whose [JavaFileExtension.rootType] matches the root's type are tried
     * for a given root.
     *
     * ### Multiple results for the same [classId]
     *
     * [findClassVirtualFiles] may return multiple results for the same [classId]. This is because a [JvmDependenciesIndex] is shared
     * between all modules and thus takes a global view on the project. A project may have two libraries which contain a class with the same
     * name. When we have two independent modules which each depend on one library, there is no classpath issue as the libraries do not
     * overlap. In such a case, the global index must provide virtual files for both classes.
     *
     * The [JvmDependenciesIndex] implementation may choose to find only the first result instead of all results if it's operated under a
     * single-module view. This avoids the possible negative performance impact of looking for multiple results.
     *
     * When finding only the first result, [acceptedExtensions] determines the order in which files are searched in each root.
     */
    fun findClassVirtualFiles(
        classId: ClassId,
        acceptedExtensions: JavaFileExtensions,
    ): Collection<VirtualFile>

    /**
     * Traverses the index for all *class* [VirtualFile]s contained in directories corresponding to the [packageFqName], and calls
     * [continueSearch] for each class [VirtualFile].
     *
     * Only files that match the [acceptedExtensions] are considered.
     *
     * In contrast to [traverseVirtualFilesInPackage], this function may rely on the same caches as [findClassVirtualFiles], and might thus
     * perform better. This depends on whether the index implementation caches results.
     *
     * @param continueSearch This function is called for each class [VirtualFile] found in the index. It should return `true` to continue
     *  the traversal and `false` to stop it.
     */
    fun traverseClassVirtualFilesInPackage(
        packageFqName: FqName,
        acceptedExtensions: JavaFileExtensions,
        continueSearch: (VirtualFile) -> Boolean,
    )

    /**
     * Traverses the index for all [VirtualFile] directories corresponding to the [packageFqName], and calls [continueSearch] for each
     * directory with its [JavaRoot.RootType].
     *
     * [acceptedRootTypes] can be used to filter the kind of roots that are considered while traversing the index.
     *
     * @param continueSearch This function is called for each [VirtualFile] directory found in the index. It should return `true` to
     *  continue the traversal and `false` to stop it.
     */
    fun traverseDirectoriesInPackage(
        packageFqName: FqName,
        acceptedRootTypes: Set<JavaRoot.RootType> = JavaRoot.SourceAndBinary,
        continueSearch: (VirtualFile, JavaRoot.RootType) -> Boolean
    )

    /**
     * Traverses the index for all [VirtualFile]s contained in directories corresponding to the [packageFqName], and calls [continueSearch]
     * for each [VirtualFile] with its [JavaRoot.RootType].
     *
     * [acceptedRootTypes] can be used to filter the kind of roots that are considered while traversing the index.
     *
     * @param continueSearch This function is called for each [VirtualFile] found in the index. It should return `true` to continue the
     *  traversal and `false` to stop it.
     */
    fun traverseVirtualFilesInPackage(
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

enum class JavaFileExtension(val extension: String, val rootType: JavaRoot.RootType) {
    JAVA(JavaFileType.DEFAULT_EXTENSION, JavaRoot.RootType.SOURCE),
    CLASS(JavaClassFileType.DEFAULT_EXTENSION, JavaRoot.RootType.BINARY),
    SIG("sig", JavaRoot.RootType.BINARY_SIG),
    KOTLIN_METADATA(METADATA_FILE_EXTENSION, JavaRoot.RootType.BINARY),
}

/**
 * A collection of [JavaFileExtension]s with pre-computed root type and extension sets.
 *
 * Instances of this class should be passed as constants to APIs of [JvmDependenciesIndex].
 *
 * @param extensions The extensions to be included in the collection. Their order might be significant, depending on the specific
 *  [JvmDependenciesIndex] implementation.
 */
class JavaFileExtensions(val extensions: Collection<JavaFileExtension>) {
    private val extensionSet = extensions.mapTo(EnumSet.noneOf(JavaFileExtension::class.java)) { it }

    private val stringExtensionSet: Set<String> = extensions.mapTo(mutableSetOf()) { it.extension }

    val rootTypes: Set<JavaRoot.RootType> = extensions.mapTo(EnumSet.noneOf(JavaRoot.RootType::class.java)) { it.rootType }

    constructor(vararg extensions: JavaFileExtension) : this(extensions.asList())

    operator fun iterator(): Iterator<JavaFileExtension> = extensions.iterator()

    operator fun contains(extension: JavaFileExtension): Boolean = extension in extensionSet

    operator fun contains(extension: String): Boolean = extension in stringExtensionSet

    fun containsAll(other: JavaFileExtensions): Boolean = this == other || other.extensions.all { it in this }

    override fun equals(other: Any?): Boolean =
        this === other || other is JavaFileExtensions && extensions == other.extensions

    override fun hashCode(): Int = extensions.hashCode()
}
