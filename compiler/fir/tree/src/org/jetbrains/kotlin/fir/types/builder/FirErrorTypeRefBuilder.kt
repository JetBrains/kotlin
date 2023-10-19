/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@FirBuilderDsl
class FirErrorTypeRefBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var annotations: MutableList<FirAnnotation> = mutableListOf()
    var type: ConeKotlinType? = null
    var delegatedTypeRef: FirTypeRef? = null
    var partiallyResolvedTypeRef: FirTypeRef? = null
    lateinit var diagnostic: ConeDiagnostic

    override fun build(): FirErrorTypeRef {
        return FirErrorTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            this.type,
            delegatedTypeRef,
            diagnostic,
            partiallyResolvedTypeRef = partiallyResolvedTypeRef,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorTypeRef(init: FirErrorTypeRefBuilder.() -> Unit): FirErrorTypeRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirErrorTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorTypeRefCopy(original: FirErrorTypeRef, init: FirErrorTypeRefBuilder.() -> Unit): FirErrorTypeRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirErrorTypeRefBuilder()
    copyBuilder.source = original.source
    copyBuilder.type = original.type
    copyBuilder.annotations = original.annotations.toMutableList()
    copyBuilder.delegatedTypeRef = original.delegatedTypeRef
    copyBuilder.diagnostic = original.diagnostic
    return copyBuilder.apply(init).build()
}
