/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Stage 2 §6.3 of `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`
 * — the deserializer-side adapter through which [JvmClassFileBasedSymbolProvider] reads
 * binary `.class` (and optionally `.sig`) files **without** routing through
 * [org.jetbrains.kotlin.fir.java.FirJavaFacade].
 *
 * When [JvmClassFileBasedSymbolProvider] is constructed with a non-null
 * `binaryClassFinderInputs`, the deserializer uses these four methods for its binary
 * lookups at the five sites flagged in the design doc table (line 72/139/171/180/212):
 *
 *  - [hasTopLevelBinaryClass] gates `computePackagePartInfo` (L72) and
 *    `extractClassMetadata` (L171). Mirrors `FirJavaFacade.hasTopLevelClassOf(classId)`
 *    semantics: returns `true` when the outermost-class name is in the known binary names
 *    of the package, or when the package's names are not enumerable.
 *  - [knownBinaryClassNamesInPackage] feeds `knownTopLevelClassesInPackage(packageFqName)`
 *    (L139). Mirrors `FirJavaFacade.knownClassNamesInPackage(packageFqName)` semantics —
 *    return `null` when the implementation cannot compute the set.
 *  - [hasBinaryPackage] gates `hasPackage(fqName)` (L212). Mirrors
 *    `FirJavaFacade.hasPackage(fqName)`.
 *  - [findBinaryClass] resolves the actual [JavaClass] at the L180 site. Implementations
 *    must apply the same `isFromSource || !hasMetadataAnnotation()` filter that
 *    `FirJavaFacade.findClass(...)` does — Kotlin classes carrying `@Metadata` must not be
 *    returned through this entry-point, otherwise the deserializer treats them as plain
 *    Java classes.
 *
 * When `binaryClassFinderInputs` is `null` (PSI / LL / IDE / scripting / IC / jklib paths),
 * the deserializer falls back to reading through `FirJavaFacade` exactly as it did before
 * §6.3 — so this interface is zero-delta for those consumers.
 *
 * The interface lives in `compiler/fir/fir-jvm/` because that's where the deserializer is.
 * The concrete `JvmDependenciesIndex` + ASM-backed implementation lives in
 * `compiler/java-direct/` (the only module with access to both). After §6.5 deletes
 * `BinaryJavaClassFinder.kt`, this adapter is the only entry-point through which the
 * deserializer reaches the binary classpath on the `java-direct` path.
 */
interface JvmBinaryClassFinderInputs {
    fun hasTopLevelBinaryClass(classId: ClassId): Boolean
    fun knownBinaryClassNamesInPackage(packageFqName: FqName): Set<String>?
    fun hasBinaryPackage(fqName: FqName): Boolean
    fun findBinaryClass(classId: ClassId, knownContent: ByteArray?): JavaClass?
}
