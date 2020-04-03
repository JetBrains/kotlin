/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirAbstractConstructorBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirPrimaryConstructorBuilder : FirAbstractConstructorBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    override lateinit var session: FirSession
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var returnTypeRef: FirTypeRef
    override var receiverTypeRef: FirTypeRef? = null
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override lateinit var status: FirDeclarationStatus
    override var containerSource: DeserializedContainerSource? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override lateinit var symbol: FirConstructorSymbol
    override var delegatedConstructor: FirDelegatedConstructorCall? = null
    override var body: FirBlock? = null

    override fun build(): FirConstructor {
        return FirPrimaryConstructor(
            source,
            session,
            resolvePhase,
            returnTypeRef,
            receiverTypeRef,
            typeParameters,
            valueParameters,
            status,
            containerSource,
            annotations,
            symbol,
            delegatedConstructor,
            body,
        )
    }


    @Deprecated("Modification of 'controlFlowGraphReference' has no impact for FirPrimaryConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var controlFlowGraphReference: FirControlFlowGraphReference
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildPrimaryConstructor(init: FirPrimaryConstructorBuilder.() -> Unit): FirConstructor {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirPrimaryConstructorBuilder().apply(init).build()
}
