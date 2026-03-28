/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirFunctionTypeRefImpl

@FirBuilderDsl
class FirFunctionTypeRefBuilder : FirAnnotationContainerBuilder {
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var source: KtSourceElement
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var receiverTypeRef: FirTypeRef? = null
    val parameters: MutableList<FirFunctionTypeParameter> = mutableListOf()
    lateinit var returnTypeRef: FirTypeRef
    var isSuspend: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val contextParameterTypeRefs: MutableList<FirTypeRef> = mutableListOf()

    override fun build(): FirFunctionTypeRef {
        return FirFunctionTypeRefImpl(
            annotations.toMutableOrEmpty(),
            source,
            isMarkedNullable,
            receiverTypeRef,
            parameters,
            returnTypeRef,
            isSuspend,
            contextParameterTypeRefs,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeRef(init: FirFunctionTypeRefBuilder.() -> Unit): FirFunctionTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirFunctionTypeRefBuilder().apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildFunctionTypeRef(
    annotations: MutableList<FirAnnotation> = mutableListOf(),
    source: KtSourceElement,
    isMarkedNullable: Boolean,
    receiverTypeRef: FirTypeRef? = null,
    parameters: MutableList<FirFunctionTypeParameter> = mutableListOf(),
    returnTypeRef: FirTypeRef,
    isSuspend: Boolean,
    contextParameterTypeRefs: MutableList<FirTypeRef> = mutableListOf(),
): FirFunctionTypeRef {
    return FirFunctionTypeRefImpl(
        annotations.toMutableOrEmpty(),
        source,
        isMarkedNullable,
        receiverTypeRef,
        parameters,
        returnTypeRef,
        isSuspend,
        contextParameterTypeRefs,
    )
}

@OptIn(FirImplementationDetail::class)
fun buildFunctionTypeRefCopy(
    original: FirFunctionTypeRef,
    annotations: MutableList<FirAnnotation> = original.annotations.toMutableList(),
    source: KtSourceElement = original.source,
    isMarkedNullable: Boolean = original.isMarkedNullable,
    receiverTypeRef: FirTypeRef? = original.receiverTypeRef,
    parameters: MutableList<FirFunctionTypeParameter> = original.parameters.toMutableList(),
    returnTypeRef: FirTypeRef = original.returnTypeRef,
    isSuspend: Boolean = original.isSuspend,
    contextParameterTypeRefs: MutableList<FirTypeRef> = original.contextParameterTypeRefs.toMutableList(),
): FirFunctionTypeRef {
    return FirFunctionTypeRefImpl(
        annotations.toMutableOrEmpty(),
        source,
        isMarkedNullable,
        receiverTypeRef,
        parameters,
        returnTypeRef,
        isSuspend,
        contextParameterTypeRefs,
    )
}
