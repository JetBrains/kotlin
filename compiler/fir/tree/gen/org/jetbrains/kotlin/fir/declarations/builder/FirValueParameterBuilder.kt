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
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
open class FirValueParameterBuilder : FirAnnotationContainerBuilder {
    open var source: KtSourceElement? = null
    open var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    open lateinit var moduleData: FirModuleData
    open lateinit var origin: FirDeclarationOrigin
    open var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    open lateinit var returnTypeRef: FirTypeRef
    open var staticReceiverParameter: FirTypeRef? = null
    open var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    open lateinit var name: Name
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    open lateinit var symbol: FirValueParameterSymbol
    open var defaultValue: FirExpression? = null
    open lateinit var containingDeclarationSymbol: FirBasedSymbol<*>
    open var isCrossinline: Boolean = false
    open var isNoinline: Boolean = false
    open var isVararg: Boolean = false
    open var valueParameterKind: FirValueParameterKind = FirValueParameterKind.Regular

    override fun build(): FirValueParameter {
        return FirValueParameterImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            returnTypeRef,
            staticReceiverParameter,
            deprecationsProvider,
            name,
            annotations.toMutableOrEmpty(),
            symbol,
            defaultValue,
            containingDeclarationSymbol,
            isCrossinline,
            isNoinline,
            isVararg,
            valueParameterKind,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameter(init: FirValueParameterBuilder.() -> Unit): FirValueParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirValueParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameterCopy(original: FirValueParameter, init: FirValueParameterBuilder.() -> Unit): FirValueParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirValueParameterBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.staticReceiverParameter = original.staticReceiverParameter
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.name = original.name
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.defaultValue = original.defaultValue
    copyBuilder.containingDeclarationSymbol = original.containingDeclarationSymbol
    copyBuilder.isCrossinline = original.isCrossinline
    copyBuilder.isNoinline = original.isNoinline
    copyBuilder.isVararg = original.isVararg
    copyBuilder.valueParameterKind = original.valueParameterKind
    return copyBuilder.apply(init).build()
}
