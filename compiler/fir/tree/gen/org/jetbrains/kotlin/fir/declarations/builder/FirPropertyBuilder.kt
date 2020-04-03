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
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParametersOwnerBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirPropertyBuilder : FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    lateinit var session: FirSession
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var returnTypeRef: FirTypeRef
    var receiverTypeRef: FirTypeRef? = null
    lateinit var name: Name
    var initializer: FirExpression? = null
    var delegate: FirExpression? = null
    var delegateFieldSymbol: FirDelegateFieldSymbol<FirProperty>? = null
    var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var getter: FirPropertyAccessor? = null
    var setter: FirPropertyAccessor? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    var containerSource: DeserializedContainerSource? = null
    lateinit var symbol: FirPropertySymbol
    var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    lateinit var status: FirDeclarationStatus

    override fun build(): FirProperty {
        return FirPropertyImpl(
            source,
            session,
            resolvePhase,
            returnTypeRef,
            receiverTypeRef,
            name,
            initializer,
            delegate,
            delegateFieldSymbol,
            isVar,
            getter,
            setter,
            annotations,
            containerSource,
            symbol,
            isLocal,
            typeParameters,
            status,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildProperty(init: FirPropertyBuilder.() -> Unit): FirProperty {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirPropertyBuilder().apply(init).build()
}
