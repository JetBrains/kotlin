/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Binary-class lookup inputs for [JvmClassFileBasedSymbolProvider] when not reading through FirJavaFacade.
 *
 * [findBinaryClass] must not return Kotlin classes with `@Metadata`; those go through the Kotlin class finder.
 */
interface JvmBinaryClassFinderInputs {
    fun hasTopLevelBinaryClass(classId: ClassId): Boolean
    fun knownBinaryClassNamesInPackage(packageFqName: FqName): Set<String>?
    fun hasBinaryPackage(fqName: FqName): Boolean
    fun findBinaryClass(classId: ClassId, knownContent: ByteArray?): JavaClass?
}
