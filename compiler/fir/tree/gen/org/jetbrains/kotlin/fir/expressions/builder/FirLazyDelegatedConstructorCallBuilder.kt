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
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef

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
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirLazyDelegatedConstructorCallBuilder().apply(init).build()
}
