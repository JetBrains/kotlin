/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirLazyDelegatedConstructorCallBuilder : FirAnnotationContainerBuilder {
    lateinit var constructedTypeRef: FirTypeRef
    lateinit var calleeReference: FirReference
    var isThis: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirDelegatedConstructorCall {
        return FirLazyDelegatedConstructorCall(
            constructedTypeRef,
            calleeReference,
            isThis,
        )
    }

    @Deprecated("Modification of 'source' has no impact for FirLazyDelegatedConstructorCallBuilder", level = DeprecationLevel.HIDDEN)
    override var source: KtSourceElement?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'annotations' has no impact for FirLazyDelegatedConstructorCallBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLazyDelegatedConstructorCall(init: FirLazyDelegatedConstructorCallBuilder.() -> Unit): FirDelegatedConstructorCall {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirLazyDelegatedConstructorCallBuilder().apply(init).build()
}
