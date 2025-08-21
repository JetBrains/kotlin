/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolIdEntry
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

abstract class FirBasedSymbol<out E : FirDeclaration> : DeclarationSymbolMarker {
    private var _fir: E? = null

    @SymbolInternals
    val fir: E
        get() = _fir
            ?: errorWithAttachment("Fir is not initialized for ${this::class}") {
                withFirSymbolIdEntry("symbol", this@FirBasedSymbol)
            }

    @FirImplementationDetail
    fun bind(e: @UnsafeVariance E) {
        _fir = e
    }

    val isBound: Boolean get() = _fir != null

    val origin: FirDeclarationOrigin
        get() = fir.origin

    val source: KtSourceElement?
        get() = fir.source

    val moduleData: FirModuleData
        get() = fir.moduleData

    @SymbolInternals
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
    if (annotations.isEmpty()) return emptyList()

    anchorElement.lazyResolveToPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
    return annotations
}

@SymbolInternals
fun FirAnnotationContainer.resolvedAnnotationsWithArguments(anchorElement: FirBasedSymbol<*>): List<FirAnnotation> {
    if (isDefinitelyEmpty(anchorElement)) return emptyList()

    if (anchorElement.fir.resolvePhase >= FirResolvePhase.ANNOTATION_ARGUMENTS) return annotations

    annotations.resolveAnnotationsWithArguments(anchorElement)

    // Note: the previous call may change this.annotations reference!
    return annotations
}

private fun List<FirAnnotation>.resolveAnnotationsWithArguments(anchorElement: FirBasedSymbol<*>) {
    // This is safe to iterate over the collection without indices since all annotations after 582b640b commit
    // declared as `MutableOrEmptyList<FirAnnotation>`, so:
    // - `replaceAnnotations` replaces the entire collection without modifications
    // - `transformAnnotations` theoretically may modify annotations, but it is not allowed due
    // to the compiler contract to change already published annotations – only their content can be changed
    val hasAnnotationCallWithArguments = any { it is FirAnnotationCall && it.arguments.isNotEmpty() }
    val phase = if (hasAnnotationCallWithArguments) {
        FirResolvePhase.ANNOTATION_ARGUMENTS
    } else {
        FirResolvePhase.TYPES
    }

    anchorElement.lazyResolveToPhase(phase)
}

private fun FirAnnotationContainer.isDefinitelyEmpty(anchorElement: FirBasedSymbol<*>): Boolean {
    if (annotations.isEmpty()) {
        if (anchorElement !is FirBackingFieldSymbol) return true
        if (anchorElement.propertySymbol.annotations.none { it.useSiteTarget == null }) return true
    }
    return false
}

@SymbolInternals
fun FirAnnotationContainer.resolvedAnnotationsWithClassIds(anchorElement: FirBasedSymbol<*>): List<FirAnnotation> {
    if (isDefinitelyEmpty(anchorElement)) return emptyList()

    anchorElement.lazyResolveToPhase(FirResolvePhase.TYPES)

    return annotations
}

private fun FirAnnotationContainer.resolvedAnnotationClassIds(anchorElement: FirBasedSymbol<*>): List<ClassId> {
    return resolvedAnnotationsWithClassIds(anchorElement).mapNotNull {
        it.annotationTypeRef.coneType.classLikeLookupTagIfAny?.classId
    }
}

@RequiresOptIn
annotation class SymbolInternals
