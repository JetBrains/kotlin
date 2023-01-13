/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
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

    val isBound get() = _fir != null

    val origin: FirDeclarationOrigin
        get() = fir.origin

    val source: KtSourceElement?
        get() = fir.source

    val moduleData: FirModuleData
        get() = fir.moduleData

    val annotations: List<FirAnnotation>
        get() = fir.annotations

    val resolvedAnnotationsWithArguments: List<FirAnnotation>
        get() = fir.resolvedAnnotationsWithArguments(this)

    val resolvedAnnotationsWithClassIds: List<FirAnnotation>
        get() = fir.resolvedAnnotationsWithClassIds(this)

    val resolvedCompilerAnnotationsWithClassIds: List<FirAnnotation>
        get() = fir.resolvedCompilerRequiredAnnotations(this)

    val resolvedAnnotationClassIds: List<ClassId>
        get() = fir.resolvedAnnotationClassIds(this)
}

@SymbolInternals
fun FirAnnotationContainer.resolvedCompilerRequiredAnnotations(anchorElement: FirBasedSymbol<*>): List<FirAnnotation> {
    anchorElement.lazyResolveToPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
    return annotations
}

@SymbolInternals
fun FirAnnotationContainer.resolvedAnnotationsWithArguments(anchorElement: FirBasedSymbol<*>): List<FirAnnotation> {
    anchorElement.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)
    return annotations
}

@SymbolInternals
fun FirAnnotationContainer.resolvedAnnotationsWithClassIds(anchorElement: FirBasedSymbol<*>): List<FirAnnotation> {
    anchorElement.lazyResolveToPhase(FirResolvePhase.TYPES)
    return annotations
}

@SymbolInternals
fun FirAnnotationContainer.resolvedAnnotationClassIds(anchorElement: FirBasedSymbol<*>): List<ClassId> {
    anchorElement.lazyResolveToPhase(FirResolvePhase.TYPES)
    return annotations.mapNotNull { (it.annotationTypeRef.coneType as? ConeClassLikeType)?.lookupTag?.classId }
}

@RequiresOptIn
annotation class SymbolInternals
