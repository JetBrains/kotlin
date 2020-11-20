/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolveStatus
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.FirCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirAnnotationCallImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAnnotationCallBuilder : FirCallBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: FirSourceElement? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var argumentList: FirArgumentList = FirEmptyArgumentList
    lateinit var calleeReference: FirReference
    var useSiteTarget: AnnotationUseSiteTarget? = null
    lateinit var annotationTypeRef: FirTypeRef
    var resolveStatus: FirAnnotationResolveStatus = FirAnnotationResolveStatus.Unresolved

    override fun build(): FirAnnotationCall {
        return FirAnnotationCallImpl(
            source,
            annotations,
            argumentList,
            calleeReference,
            useSiteTarget,
            annotationTypeRef,
            resolveStatus,
        )
    }


    @Deprecated("Modification of 'typeRef' has no impact for FirAnnotationCallBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: FirTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationCall(init: FirAnnotationCallBuilder.() -> Unit): FirAnnotationCall {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAnnotationCallBuilder().apply(init).build()
}
