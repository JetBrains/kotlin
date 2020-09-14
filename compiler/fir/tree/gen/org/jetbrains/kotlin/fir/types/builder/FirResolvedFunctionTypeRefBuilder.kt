/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedFunctionTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirResolvedFunctionTypeRefBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    lateinit var type: ConeKotlinType
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var receiverTypeRef: FirTypeRef? = null
    val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    lateinit var returnTypeRef: FirTypeRef
    var isSuspend: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): FirResolvedFunctionTypeRef {
        return FirResolvedFunctionTypeRefImpl(
            source,
            annotations,
            type,
            isMarkedNullable,
            receiverTypeRef,
            valueParameters,
            returnTypeRef,
            isSuspend,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedFunctionTypeRef(init: FirResolvedFunctionTypeRefBuilder.() -> Unit): FirResolvedFunctionTypeRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirResolvedFunctionTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedFunctionTypeRefCopy(original: FirResolvedFunctionTypeRef, init: FirResolvedFunctionTypeRefBuilder.() -> Unit): FirResolvedFunctionTypeRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirResolvedFunctionTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.type = original.type
    copyBuilder.isMarkedNullable = original.isMarkedNullable
    copyBuilder.receiverTypeRef = original.receiverTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.isSuspend = original.isSuspend
    return copyBuilder.apply(init).build()
}
