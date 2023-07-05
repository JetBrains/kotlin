/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAugmentedArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.impl.FirAugmentedArraySetCallImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAugmentedArraySetCallBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var lhsGetCall: FirFunctionCall
    lateinit var rhs: FirExpression
    lateinit var operation: FirOperation
    var calleeReference: FirReference = FirStubReference
    var arrayAccessSource: KtSourceElement? = null

    override fun build(): FirAugmentedArraySetCall {
        return FirAugmentedArraySetCallImpl(
            source,
            annotations.toMutableOrEmpty(),
            lhsGetCall,
            rhs,
            operation,
            calleeReference,
            arrayAccessSource,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAugmentedArraySetCall(init: FirAugmentedArraySetCallBuilder.() -> Unit): FirAugmentedArraySetCall {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAugmentedArraySetCallBuilder().apply(init).build()
}
