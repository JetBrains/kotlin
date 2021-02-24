/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirErrorTypeRefBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    var delegatedTypeRef: FirTypeRef? = null
    lateinit var diagnostic: ConeDiagnostic

    override fun build(): FirErrorTypeRef {
        return FirErrorTypeRefImpl(
            source,
            delegatedTypeRef,
            diagnostic,
        )
    }


    @Deprecated("Modification of 'annotations' has no impact for FirErrorTypeRefBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
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
    copyBuilder.delegatedTypeRef = original.delegatedTypeRef
    copyBuilder.diagnostic = original.diagnostic
    return copyBuilder.apply(init).build()
}
