/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.builder.FirFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAnonymousFunctionBuilder : FirFunctionBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var returnTypeRef: FirTypeRef
    var receiverTypeRef: FirTypeRef? = null
    override var deprecation: DeprecationsPerUseSite? = null
    override var containerSource: DeserializedContainerSource? = null
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    var controlFlowGraphReference: FirControlFlowGraphReference? = null
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    lateinit var symbol: FirAnonymousFunctionSymbol
    var label: FirLabel? = null
    var invocationKind: EventOccurrencesRange? = null
    var inlineStatus: InlineStatus = InlineStatus.Unknown
    var isLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var hasExplicitParameterList: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override fun build(): FirAnonymousFunction {
        return FirAnonymousFunctionImpl(
            source,
            annotations,
            moduleData,
            origin,
            attributes,
            returnTypeRef,
            receiverTypeRef,
            deprecation,
            containerSource,
            dispatchReceiverType,
            controlFlowGraphReference,
            valueParameters,
            body,
            symbol,
            label,
            invocationKind,
            inlineStatus,
            isLambda,
            hasExplicitParameterList,
            typeParameters,
            typeRef,
        )
    }


    @Deprecated("Modification of 'resolvePhase' has no impact for FirAnonymousFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var resolvePhase: FirResolvePhase
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'status' has no impact for FirAnonymousFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var status: FirDeclarationStatus
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunction(init: FirAnonymousFunctionBuilder.() -> Unit): FirAnonymousFunction {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousFunctionBuilder().apply(init).build()
}
