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
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultSetterValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

@FirBuilderDsl
class FirDefaultSetterValueParameterBuilder : FirAnnotationContainerBuilder {
    var source: KtSourceElement? = null
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var returnTypeRef: FirTypeRef
    var staticReceiverParameter: FirTypeRef? = null
    var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var symbol: FirValueParameterSymbol
    lateinit var containingDeclarationSymbol: FirBasedSymbol<*>

    override fun build(): FirValueParameter {
        return FirDefaultSetterValueParameter(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            returnTypeRef,
            staticReceiverParameter,
            deprecationsProvider,
            annotations.toMutableOrEmpty(),
            symbol,
            containingDeclarationSymbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDefaultSetterValueParameter(init: FirDefaultSetterValueParameterBuilder.() -> Unit): FirValueParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirDefaultSetterValueParameterBuilder().apply(init).build()
}
