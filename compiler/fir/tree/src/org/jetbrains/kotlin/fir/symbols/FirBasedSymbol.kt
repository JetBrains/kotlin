/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolIdFactory
import org.jetbrains.kotlin.fir.symbols.id.FirUniqueSymbolId
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolIdEntry
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * The symbol of a [FIR declaration][FirDeclaration] of type [E].
 *
 * Symbols are used to refer to declarations, and their properties and functions are the main API to access the declaration's information.
 * They ensure that the declaration is lazily resolved to the phase which the requested information requires, as for example
 * [resolvedAnnotationsWithArguments] does.
 *
 * The declaration itself is accessible via [fir]. Such direct access should be used sparingly, since it doesn't resolve the declaration:
 * the caller has to ensure that the declaration is in the required phase. Where possible, the symbol's API should be preferred.
 *
 * Each symbol has a [symbol ID][FirSymbolId] ([symbolId][org.jetbrains.kotlin.fir.symbols.symbolId]), which identifies the symbol and
 * references it. The symbol binds itself to its symbol ID on construction.
 *
 * ### Default constructors
 *
 * Each concrete symbol class has a *default constructor* which doesn't take a [symbol ID][FirSymbolId] and creates a
 * [unique symbol ID][FirUniqueSymbolId] instead. A default constructor should only be used for symbols which are stored for the lifetime of
 * the session. In particular, symbols built from PSI should not use the default constructor, but rather get their symbol ID from the
 * [FirSymbolIdFactory]. See [FirSymbolId] for more information.
 *
 * Default constructors keep existing symbol creation source-compatible, which especially eases the migration of compiler plugins.
 */
abstract class FirBasedSymbol<out E : FirDeclaration>(symbolId: FirSymbolId<FirBasedSymbol<E>>) : DeclarationSymbolMarker {
    private var _fir: E? = null

    /**
     * Backs the [symbolId][org.jetbrains.kotlin.fir.symbols.symbolId] extension, which provides the symbol ID with the precise type of the
     * symbol.
     */
    internal val backingSymbolId: FirSymbolId<FirBasedSymbol<E>> = symbolId

    @SymbolInternals
    val fir: E
        get() = _fir
            ?: errorWithAttachment("Fir is not initialized for ${this::class}") {
                withFirSymbolIdEntry("symbol", this@FirBasedSymbol)
            }

    init {
        // Once we create a symbol with a symbol ID, the symbol becomes the direct referent of the symbol ID.
        @OptIn(FirImplementationDetail::class)
        symbolId.bind(this)
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

/**
 * The [symbol ID][FirSymbolId] of this symbol, typed with the static type [S] of the symbol.
 */
val <S : FirBasedSymbol<*>> S.symbolId: FirSymbolId<S>
    get() {
        // The unchecked cast is sound: a symbol binds itself to its symbol ID on construction, so the referent of the symbol ID is an
        // instance of the symbol's own class, which is a subtype of `S`. As `FirSymbolId` is covariant, the symbol ID is thus a
        // `FirSymbolId<S>`.
        @Suppress("UNCHECKED_CAST")
        return backingSymbolId as FirSymbolId<S>
    }

/**
 * The [FirControlFlowGraphReference] of the declaration of this symbol, or `null` if the declaration is not a
 * [FirControlFlowGraphOwner], or is one which gets no graph of its own – a declaration without a body, or a local property, which is a
 * statement of its container graph.
 *
 * A graph is built during [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE], which this property requests. Reading
 * [FirControlFlowGraphOwner.controlFlowGraphReference] off the declaration directly answers `null` for a declaration which is not
 * resolved yet instead, and in the Analysis API that is the case for every declaration nobody has asked about so far.
 */
val FirBasedSymbol<*>.resolvedControlFlowGraphReference: FirControlFlowGraphReference?
    get() {
        val owner = fir as? FirControlFlowGraphOwner ?: return null
        lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return owner.controlFlowGraphReference
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

private fun FirAnnotationContainer.isDefinitelyEmpty(anchorElement: FirBasedSymbol<*>): Boolean = when {
    annotations.isNotEmpty() -> false

    // Annotations on the backing field could appear later during resolution
    anchorElement is FirBackingFieldSymbol -> anchorElement.propertySymbol.annotations.any {
        when (it.useSiteTarget) {
            null, AnnotationUseSiteTarget.FIELD, AnnotationUseSiteTarget.ALL -> false
            else -> true
        }
    }

    else -> true
}


/**
 * Whether [this] symbol might have annotations.
 *
 * - `false` means that [this] symbol definitely doesn't have annotations.
 * - `true` means that [this] symbol might have annotations.
 */
val FirBasedSymbol<*>.mightHaveAnnotations: Boolean
    get() = !fir.isDefinitelyEmpty(this)

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
