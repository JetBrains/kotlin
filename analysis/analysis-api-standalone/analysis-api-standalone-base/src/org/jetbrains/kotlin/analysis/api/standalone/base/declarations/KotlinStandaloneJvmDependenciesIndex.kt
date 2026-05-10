/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtension
import org.jetbrains.kotlin.cli.jvm.index.JavaFileExtensions
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexBase
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.SmartList
import java.util.EnumSet

/**
 * The Standalone implementation of [JvmDependenciesIndex][org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex].
 *
 * In contrast to the compiler implementation, this implementation caches the virtual files of each class for each package. While Standalone
 * workloads benefit from this optimization, this is not true for compiler workloads. So the optimization is limited to Standalone.
 *
 * The Standalone JVM dependencies index always returns all possible virtual files from [findClassVirtualFiles], as Standalone does not
 * necessarily operate under a single-module view. Since class virtual files are cached, the performance impact from finding all files
 * instead of only the first is negligible.
 */
internal class KotlinStandaloneJvmDependenciesIndex(roots: List<JavaRoot>) : JvmDependenciesIndexBase(roots) {
    /**
     * Contains a list of virtual files for every class in a package. The [String] key represents each class's relative name, separated by
     * dots (see the section below for more detail).
     *
     * The map contains all virtual files for all possible [JavaFileExtension]s. If only a subset of extensions is requested, the list has
     * to be post-processed to filter out unwanted virtual files.
     *
     * ### Relative class names
     *
     * The name of a class virtual file cannot contain `.` characters because it is not a valid file system character. Because of this,
     * nested classes have `$` characters instead of `.` as a separator in their file name. For example, `A$B.class` for a class `A.B`.
     *
     * [ClassVirtualFiles] nevertheless stores dot-separated relative class names. This is a good decision for the performance of the happy
     * path, because most relative class name strings already exist in their dot-separated forms. It would be a waste to convert `.` to `$`
     * characters each time class virtual files are requested for a [ClassId]. With dot-separated names, we only have to replace `$` with
     * `.` at cache insertion time (in [computeClassVirtualFiles]).
     *
     * However, there are edge cases with `$` characters which warrant some consideration.
     *
     * #### Handling of `$` characters
     *
     * There are legitimate uses of `$` characters in class names which are *not* related to nesting.
     *
     * Kotlin generates "anonymous" classes which nevertheless need a name in binaries. `$` is also used as a separator here because it
     * cannot clash with a user-named class. For example, `myFunction$1` might be the name of a class generated for an anonymous object used
     * in `myFunction`. Here, `$` is just a part of the class's name. In this case, the `$` -> `.` conversion is technically *not* valid.
     *
     * But the cache replaces all `$` characters with `.` because it cannot distinguish between a nested class and a `$` in the class name
     * only from the file's name. Reading the class file is not an option as it would be detrimental to performance.
     *
     * This leads to problems when requested relative class names still contain `$`, while the cache key has a `.` character in that place.
     * There are two possibilities:
     *
     * 1. We have a local class with a `$` character in the name, e.g. `myFunction$1`.
     * 2. We have a client like `findVirtualFileImprecise` in `inlineCodegenUtils.kt` that cannot distinguish between nesting and `$` as
     *    part of a class's name. Here, a nested class `foo/A.B` might be requested as `foo/A$B`.
     *
     * In both cases, the handling of `$` is simple: We replace all `$` characters with `.` in the relative class name. This is equivalent
     * to the file name which *only* contains `$` signs (even for nested classes). From that perspective, `$` and `.` are interchangeable,
     * and we choose the representation (`.`) which is best for performance.
     *
     * From a performance perspective, we rarely access local binary classes via their class name, so Standalone-based analysis should
     * almost never need to fall back to the `$` -> `.` replacement. An exception here is the compiler facility: It calls into the backend
     * which has many reasons to load local classes from binaries.
     */
    private typealias ClassVirtualFiles = Map<String, List<VirtualFile>>

    /**
     * For each package [FqName], caches the [ClassVirtualFiles] for the package.
     *
     * The cache is built on demand for each package. However, each [ClassVirtualFiles] map is built exhaustively from all roots and can be
     * used reliably.
     *
     * In the vast majority of use cases, the cache will function as if it were unlimited. The maximum size has been chosen to limit the
     * memory footprint in pathological cases.
     */
    private val classVirtualFilesByPackage =
        Caffeine.newBuilder()
            .maximumSize(CACHE_SIZE)
            .build<FqName, ClassVirtualFiles>()

    override fun findClassVirtualFiles(
        classId: ClassId,
        acceptedExtensions: JavaFileExtensions,
    ): Collection<VirtualFile> {
        val classVirtualFiles = getClassVirtualFiles(classId.packageFqName)

        val files = getVirtualFilesForClass(classId, classVirtualFiles)
        if (files.isEmpty()) return emptyList()

        // We don't need to filter the files if all extensions are requested.
        if (acceptedExtensions.extensions.size == JavaFileExtension.entries.size) {
            return files
        }

        return files.filter { file ->
            val extension = file.extension ?: return@filter false
            extension in acceptedExtensions
        }
    }

    /**
     * See [classVirtualFilesByPackage] for an explanation of the handling of relative class names.
     */
    private fun getVirtualFilesForClass(classId: ClassId, classVirtualFiles: ClassVirtualFiles): List<VirtualFile> {
        // Happy path: We can find the dot-separated relative class name in the map of class virtual files.
        val relativeClassName = classId.relativeClassName.asString()
        classVirtualFiles[relativeClassName]?.let { return it }

        if (!relativeClassName.contains('$')) {
            return emptyList()
        }

        // We replace *all* `$` characters with `.` characters even when they are part of the class name. See `ClassVirtualFiles` for a
        // deeper explanation.
        val relativeClassNameWithDots = relativeClassName.replace('$', '.')
        return classVirtualFiles[relativeClassNameWithDots] ?: emptyList()
    }

    override fun traverseClassVirtualFilesInPackage(
        packageFqName: FqName,
        acceptedExtensions: JavaFileExtensions,
        continueSearch: (VirtualFile) -> Boolean,
    ) {
        getClassVirtualFiles(packageFqName).forEach { (_, files) ->
            files.forEach { file ->
                val extension = file.extension
                if (extension != null && extension in acceptedExtensions) {
                    val shouldContinueSearch = continueSearch(file)
                    if (!shouldContinueSearch) return
                }
            }
        }
    }

    private fun getClassVirtualFiles(packageFqName: FqName): ClassVirtualFiles {
        val classVirtualFiles = classVirtualFilesByPackage.get(packageFqName, ::computeClassVirtualFiles)

        requireNotNull(classVirtualFiles) { "The map of class virtual files should always be non-null." }
        return classVirtualFiles
    }

    private fun computeClassVirtualFiles(packageFqName: FqName): ClassVirtualFiles {
        val result = HashMap<String, SmartList<VirtualFile>>()
        traverseVirtualFilesInPackage(packageFqName, ALL_ROOT_TYPES) { virtualFile, _ ->
            if (!virtualFile.isDirectory && virtualFile.extension in CACHED_EXTENSIONS) {
                val relativeClassName = virtualFile.nameWithoutExtension.replace('$', '.')
                result.getOrPut(relativeClassName, ::SmartList).add(virtualFile)
            }
            true // continue
        }
        return result
    }

    companion object {
        private const val CACHE_SIZE: Long = 5000

        private val ALL_ROOT_TYPES = EnumSet.allOf(JavaRoot.RootType::class.java)
        private val CACHED_EXTENSIONS = JavaFileExtension.entries.mapTo(HashSet()) { it.extension }
    }
}
