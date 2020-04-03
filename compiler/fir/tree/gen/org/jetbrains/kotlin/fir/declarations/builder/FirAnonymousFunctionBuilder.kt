/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousFunctionImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAnonymousFunctionBuilder : FirFunctionBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: FirSourceElement? = null
    override lateinit var session: FirSession
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override lateinit var returnTypeRef: FirTypeRef
    var receiverTypeRef: FirTypeRef? = null
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    lateinit var symbol: FirAnonymousFunctionSymbol
    var label: FirLabel? = null
    var invocationKind: InvocationKind? = null
    var isLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): FirAnonymousFunction {
        return FirAnonymousFunctionImpl(
            source,
            session,
            annotations,
            returnTypeRef,
            receiverTypeRef,
            typeParameters,
            controlFlowGraphReference,
            valueParameters,
            body,
            typeRef,
            symbol,
            label,
            invocationKind,
            isLambda,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunction(init: FirAnonymousFunctionBuilder.() -> Unit): FirAnonymousFunction {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousFunctionBuilder().apply(init).build()
}
