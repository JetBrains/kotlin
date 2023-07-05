/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirReceiverParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirReceiverParameterBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    lateinit var typeRef: FirTypeRef
    override val annotations: MutableList<FirAnnotation> = mutableListOf()

    override fun build(): FirReceiverParameter {
        return FirReceiverParameterImpl(
            source,
            typeRef,
            annotations.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildReceiverParameter(init: FirReceiverParameterBuilder.() -> Unit): FirReceiverParameter {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirReceiverParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildReceiverParameterCopy(original: FirReceiverParameter, init: FirReceiverParameterBuilder.() -> Unit): FirReceiverParameter {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirReceiverParameterBuilder()
    copyBuilder.source = original.source
    copyBuilder.typeRef = original.typeRef
    copyBuilder.annotations.addAll(original.annotations)
    return copyBuilder.apply(init).build()
}
