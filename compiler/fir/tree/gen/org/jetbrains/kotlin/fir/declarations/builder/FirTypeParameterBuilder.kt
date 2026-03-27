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
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

@FirBuilderDsl
class FirTypeParameterBuilder : FirAnnotationContainerBuilder {
    var source: KtSourceElement? = null
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var name: Name
    lateinit var symbol: FirTypeParameterSymbol
    lateinit var containingDeclarationSymbol: FirBasedSymbol<*>
    lateinit var variance: Variance
    var isReified: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val bounds: MutableList<FirTypeRef> = mutableListOf()
    override val annotations: MutableList<FirAnnotation> = mutableListOf()

    override fun build(): FirTypeParameter {
        return FirTypeParameterImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            name,
            symbol,
            containingDeclarationSymbol,
            variance,
            isReified,
            bounds.toMutableOrEmpty(),
            annotations.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeParameter(init: FirTypeParameterBuilder.() -> Unit): FirTypeParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirTypeParameterBuilder().apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildTypeParameterCopy(
    original: FirTypeParameter,
    source: KtSourceElement? = original.source,
    resolvePhase: FirResolvePhase = original.resolvePhase,
    moduleData: FirModuleData = original.moduleData,
    origin: FirDeclarationOrigin = original.origin,
    attributes: FirDeclarationAttributes = original.attributes.copy(),
    name: Name = original.name,
    symbol: FirTypeParameterSymbol,
    containingDeclarationSymbol: FirBasedSymbol<*> = original.containingDeclarationSymbol,
    variance: Variance = original.variance,
    isReified: Boolean = original.isReified,
    bounds: MutableList<FirTypeRef> = original.bounds.toMutableList(),
    annotations: MutableList<FirAnnotation> = original.annotations.toMutableList(),
): FirTypeParameter {
    return FirTypeParameterImpl(
        source,
        resolvePhase,
        moduleData,
        origin,
        attributes,
        name,
        symbol,
        containingDeclarationSymbol,
        variance,
        isReified,
        bounds.toMutableOrEmpty(),
        annotations.toMutableOrEmpty(),
    )
}
