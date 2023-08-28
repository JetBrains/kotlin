/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
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
    var type: ConeKotlinType? = null
    var delegatedTypeRef: FirTypeRef? = null
    var partiallyResolvedTypeRef: FirTypeRef? = null

    lateinit var diagnostic: ConeDiagnostic

    override fun build(): FirErrorTypeRef {
        val type = this.type
        return if (type != null) {
            FirErrorTypeRefImpl(
                source,
                type,
                delegatedTypeRef,
                diagnostic,
                partiallyResolvedTypeRef = partiallyResolvedTypeRef,
            )
        } else {
            FirErrorTypeRefImpl(
                source,
                delegatedTypeRef,
                diagnostic,
                partiallyResolvedTypeRef = partiallyResolvedTypeRef,
            )
        }
    }

    @Deprecated("Modification of 'annotations' has no impact for FirErrorTypeRefBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
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
    copyBuilder.delegatedTypeRef = original.delegatedTypeRef
    copyBuilder.diagnostic = original.diagnostic
    return copyBuilder.apply(init).build()
}
