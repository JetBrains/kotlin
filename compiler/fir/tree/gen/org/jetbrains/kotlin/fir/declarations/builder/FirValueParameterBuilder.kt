/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
open class FirValueParameterBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    open lateinit var session: FirSession
    open var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    open lateinit var origin: FirDeclarationOrigin
    open lateinit var returnTypeRef: FirTypeRef
    open lateinit var name: Name
    open lateinit var symbol: FirVariableSymbol<FirValueParameter>
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    open var defaultValue: FirExpression? = null
    open var isCrossinline: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    open var isNoinline: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    open var isVararg: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirValueParameter {
        return FirValueParameterImpl(
            source,
            session,
            resolvePhase,
            origin,
            returnTypeRef,
            name,
            symbol,
            annotations,
            defaultValue,
            isCrossinline,
            isNoinline,
            isVararg,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameter(init: FirValueParameterBuilder.() -> Unit): FirValueParameter {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirValueParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameterCopy(original: FirValueParameter, init: FirValueParameterBuilder.() -> Unit): FirValueParameter {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirValueParameterBuilder()
    copyBuilder.source = original.source
    copyBuilder.session = original.session
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.origin = original.origin
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.name = original.name
    copyBuilder.symbol = original.symbol
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.defaultValue = original.defaultValue
    copyBuilder.isCrossinline = original.isCrossinline
    copyBuilder.isNoinline = original.isNoinline
    copyBuilder.isVararg = original.isVararg
    return copyBuilder.apply(init).build()
}
