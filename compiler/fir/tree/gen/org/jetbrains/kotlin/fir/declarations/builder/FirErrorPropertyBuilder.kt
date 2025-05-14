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
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirErrorPropertyImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
class FirErrorPropertyBuilder : FirVariableBuilder, FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override var staticReceiverParameter: FirTypeRef? = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var containerSource: DeserializedContainerSource? = null
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    override val contextParameters: MutableList<FirValueParameter> = mutableListOf()
    override lateinit var name: Name
    override var initializer: FirExpression? = null
    override var backingField: FirBackingField? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    var delegateFieldSymbol: FirDelegateFieldSymbol? = null
    var isLocal: Boolean = false
    var bodyResolveState: FirPropertyBodyResolveState = FirPropertyBodyResolveState.NOTHING_RESOLVED
    lateinit var diagnostic: ConeDiagnostic
    lateinit var symbol: FirErrorPropertySymbol

    override fun build(): FirErrorProperty {
        return FirErrorPropertyImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            staticReceiverParameter,
            deprecationsProvider,
            containerSource,
            dispatchReceiverType,
            contextParameters.toMutableOrEmpty(),
            name,
            initializer,
            backingField,
            annotations.toMutableOrEmpty(),
            delegateFieldSymbol,
            isLocal,
            bodyResolveState,
            diagnostic,
            symbol,
        )
    }


    @Deprecated("Modification of 'status' has no impact for FirErrorPropertyBuilder", level = DeprecationLevel.HIDDEN)
    override var status: FirDeclarationStatus
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'returnTypeRef' has no impact for FirErrorPropertyBuilder", level = DeprecationLevel.HIDDEN)
    override var returnTypeRef: FirTypeRef
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'receiverParameter' has no impact for FirErrorPropertyBuilder", level = DeprecationLevel.HIDDEN)
    override var receiverParameter: FirReceiverParameter?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'delegate' has no impact for FirErrorPropertyBuilder", level = DeprecationLevel.HIDDEN)
    override var delegate: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'isVar' has no impact for FirErrorPropertyBuilder", level = DeprecationLevel.HIDDEN)
    override var isVar: Boolean
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'getter' has no impact for FirErrorPropertyBuilder", level = DeprecationLevel.HIDDEN)
    override var getter: FirPropertyAccessor?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'setter' has no impact for FirErrorPropertyBuilder", level = DeprecationLevel.HIDDEN)
    override var setter: FirPropertyAccessor?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'typeParameters' has no impact for FirErrorPropertyBuilder", level = DeprecationLevel.HIDDEN)
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorProperty(init: FirErrorPropertyBuilder.() -> Unit): FirErrorProperty {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirErrorPropertyBuilder().apply(init).build()
}
