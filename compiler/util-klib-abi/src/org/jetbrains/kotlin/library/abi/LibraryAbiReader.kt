/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.abi.impl.LibraryAbiReaderImpl
import java.io.File

/** The default implementation of [LibraryAbi] reader. */
@ExperimentalLibraryAbiReader
object LibraryAbiReader {
    /**
     * Inspect the KLIB at [library]. The KLIB can be either in a directory (unzipped) or in a file (zipped) form.
     *
     * @param library The file representing the KLIB location.
     * @param filters The filters that are applied while reading the KLIB to exclude/ignore certain entities.
     */
    fun readAbiInfo(library: File, vararg filters: AbiReadingFilter): LibraryAbi = readAbiInfo(library, filters.asList())

    /** @see [readAbiInfo] */
    fun readAbiInfo(library: File, filters: List<AbiReadingFilter>): LibraryAbi = LibraryAbiReaderImpl(library, filters).readAbi()
}

/**
 * Defines a filter for skipping certain [AbiDeclaration]s during reading the KLIB ABI.
 *
 * There are several ready-to-use implementations: [ExcludedPackages], [ExcludedClasses] and [NonPublicMarkerAnnotations].
 * But also it's possible to implement your own filter if necessary.
 */
@ExperimentalLibraryAbiReader
interface AbiReadingFilter {
    /** Tests for each package being read by the ABI reader if it should be excluded/ignored. */
    fun isPackageExcluded(packageName: AbiCompoundName): Boolean = false

    /** Tests for each declaration being read by the ABI reader if it should be excluded/ignored */
    fun isDeclarationExcluded(declaration: AbiDeclaration): Boolean = false

    /** The default implementation of a filter that ignores certain packages. */
    class ExcludedPackages(excludedPackageNames: Collection<AbiCompoundName>) : AbiReadingFilter {
        private val excludedPackageNames = excludedPackageNames.toSet()

        override fun isPackageExcluded(packageName: AbiCompoundName) = when {
            excludedPackageNames.isEmpty() -> false
            packageName in excludedPackageNames -> true
            else -> excludedPackageNames.any { excludedPackageName -> excludedPackageName isContainerOf packageName }
        }
    }

    /** The default implementation of a filter that ignores certain classes. */
    class ExcludedClasses(excludedClassNames: Collection<AbiQualifiedName>) : AbiReadingFilter {
        private val excludedClassNames = excludedClassNames.toSet()

        override fun isDeclarationExcluded(declaration: AbiDeclaration) =
            declaration is AbiClass && declaration.qualifiedName in excludedClassNames
    }

    /**
     * The default implementation of a filter that ignores declarations that have at least
     * one annotation from [nonPublicMarkerNames] list.
     */
    class NonPublicMarkerAnnotations(nonPublicMarkerNames: Collection<AbiQualifiedName>) : AbiReadingFilter {
        private val nonPublicMarkerNames = nonPublicMarkerNames.toSet().toTypedArray()

        override fun isDeclarationExcluded(declaration: AbiDeclaration): Boolean {
            for (nonPublicMarkerName in nonPublicMarkerNames) {
                if (declaration.hasAnnotation(nonPublicMarkerName)
                    || (declaration as? AbiProperty)?.backingField?.hasAnnotation(nonPublicMarkerName) == true
                ) {
                    return true
                }
            }
            return false
        }
    }

    /** The default composite filter implementation: Exposes multiple incorporated filters as a single filter. */
    class Composite(filters: List<AbiReadingFilter>) : AbiReadingFilter {
        private val filters = filters.toTypedArray()

        override fun isPackageExcluded(packageName: AbiCompoundName): Boolean {
            for (filter in filters) {
                if (filter.isPackageExcluded(packageName)) return true
            }
            return false
        }

        override fun isDeclarationExcluded(declaration: AbiDeclaration): Boolean {
            for (filter in filters) {
                if (filter.isDeclarationExcluded(declaration)) return true
            }
            return false
        }
    }
}
