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
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirDelegatedConstructorCallImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef

@FirBuilderDsl
class FirDelegatedConstructorCallBuilder : FirCallBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override var argumentList: FirArgumentList = FirEmptyArgumentList
    val contextArguments: MutableList<FirExpression> = mutableListOf()
    lateinit var constructedTypeRef: FirTypeRef
    var dispatchReceiver: FirExpression? = null
    lateinit var calleeReference: FirReference
    override var source: KtSourceElement? = null
    var isThis: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): FirDelegatedConstructorCall {
        return FirDelegatedConstructorCallImpl(
            annotations.toMutableOrEmpty(),
            argumentList,
            contextArguments.toMutableOrEmpty(),
            constructedTypeRef,
            dispatchReceiver,
            calleeReference,
            source,
            isThis,
        )
    }


    @Deprecated("Modification of 'coneTypeOrNull' has no impact for FirDelegatedConstructorCallBuilder", level = DeprecationLevel.HIDDEN)
    override var coneTypeOrNull: ConeKotlinType?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegatedConstructorCall(init: FirDelegatedConstructorCallBuilder.() -> Unit): FirDelegatedConstructorCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirDelegatedConstructorCallBuilder().apply(init).build()
}
