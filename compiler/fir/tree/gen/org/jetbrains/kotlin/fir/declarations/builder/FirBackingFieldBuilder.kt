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
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirBackingFieldImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
class FirBackingFieldBuilder : FirVariableBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var returnTypeRef: FirTypeRef
    override var staticReceiverParameter: FirTypeRef? = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override lateinit var name: Name
    override var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isVal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var symbol: FirBackingFieldSymbol
    lateinit var propertySymbol: FirPropertySymbol
    override var initializer: FirExpression? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var status: FirDeclarationStatus

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirBackingField {
        return FirBackingFieldImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            returnTypeRef,
            staticReceiverParameter,
            deprecationsProvider,
            name,
            isVar,
            isVal,
            symbol,
            propertySymbol,
            initializer,
            annotations.toMutableOrEmpty(),
            status,
        )
    }


    @Deprecated("Modification of 'receiverParameter' has no impact for FirBackingFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var receiverParameter: FirReceiverParameter?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'containerSource' has no impact for FirBackingFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var containerSource: DeserializedContainerSource?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'dispatchReceiverType' has no impact for FirBackingFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var dispatchReceiverType: ConeSimpleKotlinType?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'contextParameters' has no impact for FirBackingFieldBuilder", level = DeprecationLevel.HIDDEN)
    override val contextParameters: MutableList<FirValueParameter> = mutableListOf()

    @Deprecated("Modification of 'delegate' has no impact for FirBackingFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var delegate: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'getter' has no impact for FirBackingFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var getter: FirPropertyAccessor?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'setter' has no impact for FirBackingFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var setter: FirPropertyAccessor?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'backingField' has no impact for FirBackingFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var backingField: FirBackingField?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildBackingField(init: FirBackingFieldBuilder.() -> Unit): FirBackingField {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirBackingFieldBuilder().apply(init).build()
}
