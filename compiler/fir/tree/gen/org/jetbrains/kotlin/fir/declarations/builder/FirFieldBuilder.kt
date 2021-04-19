/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirDeclarationBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirFieldImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
open class FirFieldBuilder : FirDeclarationBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    override lateinit var session: FirSession
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    open lateinit var returnTypeRef: FirTypeRef
    open lateinit var name: Name
    open lateinit var symbol: FirVariableSymbol<FirField>
    open var initializer: FirExpression? = null
    open var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    open val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    open lateinit var status: FirDeclarationStatus
    open var containerSource: DeserializedContainerSource? = null
    open var dispatchReceiverType: ConeKotlinType? = null

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirField {
        return FirFieldImpl(
            source,
            session,
            resolvePhase,
            origin,
            attributes,
            returnTypeRef,
            name,
            symbol,
            initializer,
            isVar,
            annotations,
            typeParameters,
            status,
            containerSource,
            dispatchReceiverType,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildField(init: FirFieldBuilder.() -> Unit): FirField {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirFieldBuilder().apply(init).build()
}
