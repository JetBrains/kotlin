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
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.FirContractFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParametersOwnerBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirContractFunctionImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirContractFunctionBuilder : FirFunctionBuilder, FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    override lateinit var session: FirSession
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var origin: FirDeclarationOrigin
    override lateinit var returnTypeRef: FirTypeRef
    var receiverTypeRef: FirTypeRef? = null
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    lateinit var status: FirDeclarationStatus
    var containerSource: DeserializedContainerSource? = null
    var contractDescription: FirContractDescription = FirEmptyContractDescription
    lateinit var name: Name
    lateinit var symbol: FirFunctionSymbol<FirContractFunction>
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()

    override fun build(): FirContractFunction {
        return FirContractFunctionImpl(
            source,
            session,
            resolvePhase,
            origin,
            returnTypeRef,
            receiverTypeRef,
            valueParameters,
            body,
            status,
            containerSource,
            contractDescription,
            name,
            symbol,
            annotations,
            typeParameters,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildContractFunction(init: FirContractFunctionBuilder.() -> Unit): FirContractFunction {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirContractFunctionBuilder().apply(init).build()
}
