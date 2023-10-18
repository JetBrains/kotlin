/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolIdEntry
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

abstract class FirBasedSymbol<E : FirDeclaration> : DeclarationSymbolMarker {
    private var _fir: E? = null

    @SymbolInternals
    val fir: E
        get() = _fir
            ?: errorWithAttachment("Fir is not initialized for ${this::class}") {
                withFirSymbolIdEntry("symbol", this@FirBasedSymbol)
            }

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
    if (annotations.isEmpty()) return emptyList()

    anchorElement.lazyResolveToPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
    return annotations
}

@SymbolInternals
fun FirAnnotationContainer.resolvedAnnotationsWithArguments(anchorElement: FirBasedSymbol<*>): List<FirAnnotation> {
    if (isDefinitelyEmpty(anchorElement)) return emptyList()

    annotations.resolveAnnotationsWithArguments(anchorElement)
    // Note: this.annotations reference may be changed by the previous call!
    return annotations
}

@SymbolInternals
fun List<FirAnnotation>.resolveAnnotationsWithArguments(anchorElement: FirBasedSymbol<*>) {
    /**
     * This loop by index is required to avoid possible [ConcurrentModificationException],
     * because the annotations might be in a process of resolve from some other threads
     */
    var hasAnnotationCallWithArguments = false
    for (i in indices) {
        val currentAnnotation = get(i)
        if (currentAnnotation is FirAnnotationCall && currentAnnotation.arguments.isNotEmpty()) {
            hasAnnotationCallWithArguments = true
            break
        }
    }

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

@SymbolInternals
fun resolveAnnotationsWithClassIds(anchorElement: FirBasedSymbol<*>) {
    anchorElement.lazyResolveToPhase(FirResolvePhase.TYPES)
}

@SymbolInternals
fun FirAnnotationContainer.resolvedAnnotationClassIds(anchorElement: FirBasedSymbol<*>): List<ClassId> {
    return resolvedAnnotationsWithClassIds(anchorElement).mapNotNull {
        (it.annotationTypeRef.coneType as? ConeClassLikeType)?.lookupTag?.classId
    }
}

@RequiresOptIn
annotation class SymbolInternals
