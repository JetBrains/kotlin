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
import org.jetbrains.kotlin.fir.declarations.impl.FirFieldImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
open class FirFieldBuilder : FirVariableBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var status: FirDeclarationStatus
    override lateinit var returnTypeRef: FirTypeRef
    override var staticReceiverParameter: FirTypeRef? = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    override lateinit var name: Name
    override var initializer: FirExpression? = null
    override var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
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
            status,
            returnTypeRef,
            staticReceiverParameter,
            deprecationsProvider,
            dispatchReceiverType,
            name,
            initializer,
            isVar,
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

    @Deprecated("Modification of 'containerSource' has no impact for FirFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var containerSource: DeserializedContainerSource?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'contextParameters' has no impact for FirFieldBuilder", level = DeprecationLevel.HIDDEN)
    override val contextParameters: MutableList<FirValueParameter> = mutableListOf()

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

    @Deprecated("Modification of 'backingField' has no impact for FirFieldBuilder", level = DeprecationLevel.HIDDEN)
    override var backingField: FirBackingField?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildField(init: FirFieldBuilder.() -> Unit): FirField {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirFieldBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFieldCopy(original: FirField, init: FirFieldBuilder.() -> Unit): FirField {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirFieldBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.status = original.status
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.staticReceiverParameter = original.staticReceiverParameter
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.name = original.name
    copyBuilder.initializer = original.initializer
    copyBuilder.isVar = original.isVar
    copyBuilder.annotations.addAll(original.annotations)
    return copyBuilder.apply(init).build()
}
