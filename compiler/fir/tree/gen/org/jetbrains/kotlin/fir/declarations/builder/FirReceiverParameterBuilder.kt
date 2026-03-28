/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirReceiverParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

@FirBuilderDsl
class FirReceiverParameterBuilder : FirAnnotationContainerBuilder {
    var source: KtSourceElement? = null
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var symbol: FirReceiverParameterSymbol
    lateinit var typeRef: FirTypeRef
    lateinit var containingDeclarationSymbol: FirBasedSymbol<*>
    override val annotations: MutableList<FirAnnotation> = mutableListOf()

    override fun build(): FirReceiverParameter {
        return FirReceiverParameterImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            symbol,
            typeRef,
            containingDeclarationSymbol,
            annotations.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildReceiverParameter(init: FirReceiverParameterBuilder.() -> Unit): FirReceiverParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirReceiverParameterBuilder().apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildReceiverParameter(
    source: KtSourceElement? = null,
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    attributes: FirDeclarationAttributes = FirDeclarationAttributes(),
    symbol: FirReceiverParameterSymbol,
    typeRef: FirTypeRef,
    containingDeclarationSymbol: FirBasedSymbol<*>,
    annotations: MutableList<FirAnnotation> = mutableListOf(),
): FirReceiverParameter {
    return FirReceiverParameterImpl(
        source,
        resolvePhase,
        moduleData,
        origin,
        attributes,
        symbol,
        typeRef,
        containingDeclarationSymbol,
        annotations.toMutableOrEmpty(),
    )
}

@OptIn(FirImplementationDetail::class)
fun buildReceiverParameterCopy(
    original: FirReceiverParameter,
    source: KtSourceElement? = original.source,
    resolvePhase: FirResolvePhase = original.resolvePhase,
    moduleData: FirModuleData = original.moduleData,
    origin: FirDeclarationOrigin = original.origin,
    attributes: FirDeclarationAttributes = original.attributes.copy(),
    symbol: FirReceiverParameterSymbol,
    typeRef: FirTypeRef = original.typeRef,
    containingDeclarationSymbol: FirBasedSymbol<*> = original.containingDeclarationSymbol,
    annotations: MutableList<FirAnnotation> = original.annotations.toMutableList(),
): FirReceiverParameter {
    return FirReceiverParameterImpl(
        source,
        resolvePhase,
        moduleData,
        origin,
        attributes,
        symbol,
        typeRef,
        containingDeclarationSymbol,
        annotations.toMutableOrEmpty(),
    )
}
