/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Allows compiler plugins to register simple-name to [ClassId] mappings for classes that Java PSI cannot resolve
 * (e.g., Lombok-generated builder classes that do not appear in Java source).
 *
 * Used by JavaTypeConversion to recover the correct nested ClassId when the PSI classifier is null.
 */
interface FirUnresolvedJavaClassNamesProvider : FirSessionComponent {
    fun findClassIdBySimpleName(simpleName: Name): ClassId?
    fun registerSimpleNameAlias(simpleName: Name, classId: ClassId)
}

val FirSession.unresolvedJavaClassNamesProvider: FirUnresolvedJavaClassNamesProvider?
    by FirSession.nullableSessionComponentAccessor()
