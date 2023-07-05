/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolveState
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.declarations.asResolveState
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirTypeParameterBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
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
            bounds,
            annotations.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeParameter(init: FirTypeParameterBuilder.() -> Unit): FirTypeParameter {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirTypeParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeParameterCopy(original: FirTypeParameter, init: FirTypeParameterBuilder.() -> Unit): FirTypeParameter {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirTypeParameterBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.name = original.name
    copyBuilder.symbol = original.symbol
    copyBuilder.containingDeclarationSymbol = original.containingDeclarationSymbol
    copyBuilder.variance = original.variance
    copyBuilder.isReified = original.isReified
    copyBuilder.bounds.addAll(original.bounds)
    copyBuilder.annotations.addAll(original.annotations)
    return copyBuilder.apply(init).build()
}
