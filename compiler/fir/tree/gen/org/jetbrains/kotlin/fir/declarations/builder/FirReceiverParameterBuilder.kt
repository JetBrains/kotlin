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

@OptIn(ExperimentalContracts::class)
inline fun buildReceiverParameterCopy(original: FirReceiverParameter, init: FirReceiverParameterBuilder.() -> Unit): FirReceiverParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirReceiverParameterBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.typeRef = original.typeRef
    copyBuilder.containingDeclarationSymbol = original.containingDeclarationSymbol
    copyBuilder.annotations.addAll(original.annotations)
    return copyBuilder.apply(init).build()
}
