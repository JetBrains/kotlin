/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirAnnotationCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource

@FirBuilderDsl
class FirAnnotationCallBuilder : FirCallBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    var useSiteTarget: AnnotationUseSiteTarget? = null
    var annotationTypeRef: FirTypeRef = FirImplicitTypeRefImplWithoutSource
    val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var argumentList: FirArgumentList = FirEmptyArgumentList
    lateinit var calleeReference: FirReference
    var argumentMapping: FirAnnotationArgumentMapping = FirEmptyAnnotationArgumentMapping
    var annotationResolvePhase: FirAnnotationResolvePhase = FirAnnotationResolvePhase.Unresolved
    lateinit var containingDeclarationSymbol: FirBasedSymbol<*>

    override fun build(): FirAnnotationCall {
        return FirAnnotationCallImpl(
            source,
            useSiteTarget,
            annotationTypeRef,
            typeArguments.toMutableOrEmpty(),
            argumentList,
            calleeReference,
            argumentMapping,
            annotationResolvePhase,
            containingDeclarationSymbol,
        )
    }


    @Deprecated("Modification of 'coneTypeOrNull' has no impact for FirAnnotationCallBuilder", level = DeprecationLevel.HIDDEN)
    override var coneTypeOrNull: ConeKotlinType?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'annotations' has no impact for FirAnnotationCallBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationCall(init: FirAnnotationCallBuilder.() -> Unit): FirAnnotationCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirAnnotationCallBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationCallCopy(original: FirAnnotationCall, init: FirAnnotationCallBuilder.() -> Unit): FirAnnotationCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirAnnotationCallBuilder()
    copyBuilder.source = original.source
    copyBuilder.useSiteTarget = original.useSiteTarget
    copyBuilder.annotationTypeRef = original.annotationTypeRef
    copyBuilder.typeArguments.addAll(original.typeArguments)
    copyBuilder.argumentList = original.argumentList
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.argumentMapping = original.argumentMapping
    copyBuilder.annotationResolvePhase = original.annotationResolvePhase
    copyBuilder.containingDeclarationSymbol = original.containingDeclarationSymbol
    return copyBuilder.apply(init).build()
}
