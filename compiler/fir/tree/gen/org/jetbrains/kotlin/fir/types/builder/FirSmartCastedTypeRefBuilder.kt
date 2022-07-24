/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirSmartCastedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirSmartCastedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.types.SmartcastStability

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirSmartCastedTypeRefBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    var delegatedTypeRef: FirTypeRef? = null
    var isFromStubType: Boolean = false
    lateinit var typesFromSmartCast: Collection<ConeKotlinType>
    lateinit var originalType: ConeKotlinType
    lateinit var smartcastType: ConeKotlinType
    lateinit var smartcastStability: SmartcastStability

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirSmartCastedTypeRef {
        return FirSmartCastedTypeRefImpl(
            source,
            annotations,
            delegatedTypeRef,
            isFromStubType,
            typesFromSmartCast,
            originalType,
            smartcastType,
            smartcastStability,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSmartCastedTypeRef(init: FirSmartCastedTypeRefBuilder.() -> Unit): FirSmartCastedTypeRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirSmartCastedTypeRefBuilder().apply(init).build()
}
