/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.DeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolveState
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.declarations.UnresolvedDeprecationProvider
import org.jetbrains.kotlin.fir.declarations.asResolveState
import org.jetbrains.kotlin.fir.declarations.builder.FirVariableBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirFieldImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
open class FirFieldBuilder : FirVariableBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    open val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    override lateinit var status: FirDeclarationStatus
    override lateinit var returnTypeRef: FirTypeRef
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var containerSource: DeserializedContainerSource? = null
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    override val contextReceivers: MutableList<FirContextReceiver> = mutableListOf()
    override lateinit var name: Name
    override var initializer: FirExpression? = null
    override var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override var backingField: FirBackingField? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    open lateinit var symbol: FirFieldSymbol

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirField {
        return FirFieldImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            typeParameters,
            status,
            returnTypeRef,
            deprecationsProvider,
            containerSource,
            dispatchReceiverType,
            contextReceivers.toMutableOrEmpty(),
            name,
            initializer,
            isVar,
            backingField,
            annotations.toMutableOrEmpty(),
            symbol,
        )
    }


    @Deprecated("Modification of 'receiverParameter' has no impact for FirFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var receiverParameter: FirReceiverParameter?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'delegate' has no impact for FirFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var delegate: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'getter' has no impact for FirFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var getter: FirPropertyAccessor?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'setter' has no impact for FirFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var setter: FirPropertyAccessor?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildField(init: FirFieldBuilder.() -> Unit): FirField {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirFieldBuilder().apply(init).build()
}
