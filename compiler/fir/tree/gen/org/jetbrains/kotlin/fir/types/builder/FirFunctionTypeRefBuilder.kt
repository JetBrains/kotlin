/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirFunctionTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirFunctionTypeRefBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var receiverTypeRef: FirTypeRef? = null
    val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    lateinit var returnTypeRef: FirTypeRef
    var isSuspend: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val contextReceiverTypeRefs: MutableList<FirTypeRef> = mutableListOf()

    override fun build(): FirFunctionTypeRef {
        return FirFunctionTypeRefImpl(
            source,
            annotations,
            isMarkedNullable,
            receiverTypeRef,
            valueParameters,
            returnTypeRef,
            isSuspend,
            contextReceiverTypeRefs,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeRef(init: FirFunctionTypeRefBuilder.() -> Unit): FirFunctionTypeRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirFunctionTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeRefCopy(original: FirFunctionTypeRef, init: FirFunctionTypeRefBuilder.() -> Unit): FirFunctionTypeRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirFunctionTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.isMarkedNullable = original.isMarkedNullable
    copyBuilder.receiverTypeRef = original.receiverTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.isSuspend = original.isSuspend
    copyBuilder.contextReceiverTypeRefs.addAll(original.contextReceiverTypeRefs)
    return copyBuilder.apply(init).build()
}
