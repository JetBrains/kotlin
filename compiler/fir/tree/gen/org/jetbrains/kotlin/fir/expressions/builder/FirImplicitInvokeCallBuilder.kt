/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirImplicitInvokeCallImpl
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection

@FirBuilderDsl
open class FirImplicitInvokeCallBuilder : FirAbstractFunctionCallBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override val contextReceiverArguments: MutableList<FirExpression> = mutableListOf()
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var explicitReceiver: FirExpression? = null
    override var dispatchReceiver: FirExpression? = null
    override var extensionReceiver: FirExpression? = null
    override var source: KtSourceElement? = null
    override val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()
    override var argumentList: FirArgumentList = FirEmptyArgumentList
    override lateinit var calleeReference: FirNamedReference
    open var isCallWithExplicitReceiver: Boolean = false

    override fun build(): FirImplicitInvokeCall {
        return FirImplicitInvokeCallImpl(
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            contextReceiverArguments.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            source,
            nonFatalDiagnostics.toMutableOrEmpty(),
            argumentList,
            calleeReference,
            isCallWithExplicitReceiver,
        )
    }


    @Deprecated("Modification of 'origin' has no impact for FirImplicitInvokeCallBuilder", level = DeprecationLevel.HIDDEN)
    override var origin: FirFunctionCallOrigin
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildImplicitInvokeCall(init: FirImplicitInvokeCallBuilder.() -> Unit): FirImplicitInvokeCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirImplicitInvokeCallBuilder().apply(init).build()
}
