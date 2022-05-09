/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId

abstract class FirBasedSymbol<E : FirDeclaration> {
    private var _fir: E? = null

    @SymbolInternals
    val fir: E
        get() = _fir
            ?: error("Fir is not initialized for $this")

    fun bind(e: E) {
        _fir = e
    }

    val origin: FirDeclarationOrigin
        get() = fir.origin

    val source: KtSourceElement?
        get() = fir.source

    val moduleData: FirModuleData
        get() = fir.moduleData

    val annotations: List<FirAnnotation>
        get() = fir.annotations

    val resolvedAnnotationsWithArguments: List<FirAnnotation>
        get() {
            ensureResolved(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)
            return fir.annotations
        }

    val resolvedAnnotationsWithClassIds: List<FirAnnotation>
        get() {
            ensureResolved(FirResolvePhase.TYPES)
            return fir.annotations
        }

    val resolvedAnnotationClassIds: List<ClassId>
        get() {
            ensureResolved(FirResolvePhase.TYPES)
            return fir.annotations.mapNotNull { (it.annotationTypeRef.coneType as? ConeClassLikeType)?.lookupTag?.classId }
        }
}

@RequiresOptIn
annotation class SymbolInternals
