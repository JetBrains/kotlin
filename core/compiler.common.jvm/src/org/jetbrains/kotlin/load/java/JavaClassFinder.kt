/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface JavaClassFinder {
    data class Request(
        val classId: ClassId,
        @Suppress("ArrayInDataClass")
        val previouslyFoundClassFileContent: ByteArray? = null,
        val outerClass: JavaClass? = null
    )

    fun findClass(request: Request): JavaClass?
    fun findClass(classId: ClassId): JavaClass? = findClass(Request(classId))

    /**
     * Finds all classes with the specified [ClassId]. This function should be used if the search space permits such ambiguities and if
     * [findClass] is not guaranteed to disambiguate by itself. For example, in an IDE context, a broad search scope might lead to multiple
     * valid candidates, which need to be disambiguated according to classpath order.
     *
     * [findClasses] may return a single [JavaClass], even if more could be found, if the resulting [JavaClass] is guaranteed to be the
     * first in the dependency order.
     */
    fun findClasses(request: Request): List<JavaClass>

    fun findClasses(classId: ClassId): List<JavaClass> = findClasses(Request(classId))

    fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean = true): JavaPackage?

    fun knownClassNamesInPackage(packageFqName: FqName): Set<String>?

    /**
     * Whether [knownClassNamesInPackage] can be computed. When [canComputeKnownClassNamesInPackage] is `false`, [knownClassNamesInPackage]
     * will always return `null`.
     */
    fun canComputeKnownClassNamesInPackage(): Boolean

    // ---- Source-only probes (Stage 2 §6.2 of `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`) ----
    //
    // These let `JavaSymbolProvider` see *only* the Java source half of a finder, leaving binary
    // Java lookups to flow through `JvmClassFileBasedSymbolProvider`. For non-combined finders
    // (PSI, reflect, javac, plain binary) the defaults coincide with the existing methods, since
    // those finders are themselves a single "side" — the narrowing is a no-op for them.
    //
    // `CombinedJavaClassFinder` overrides these to delegate to its `sourceFinder` only.

    /**
     * Cheap (index-level) check whether [classId] could be served by the Java source half of this finder.
     * Used by `JavaSymbolProvider.getClassLikeSymbolByClassId` as a gate so binary classes flow through
     * `JvmClassFileBasedSymbolProvider` instead.
     *
     * Default = `true` (preserving current behavior for finders that have no separate "source side").
     */
    fun isInSourceIndex(classId: ClassId): Boolean = true

    /**
     * Whether [fqName] is a Java package known to the source half of this finder.
     * Default = `findPackage(fqName, mayHaveAnnotations = false) != null`.
     */
    fun hasPackageInSources(fqName: FqName): Boolean = findPackage(fqName, mayHaveAnnotations = false) != null

    /**
     * Top-level Java class names visible to the source half of this finder, or `null` if not computable.
     * Default = [knownClassNamesInPackage] (same as today for single-side finders).
     */
    fun sourceClassNamesInPackage(packageFqName: FqName): Set<String>? = knownClassNamesInPackage(packageFqName)
}
