/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirFunctionTypeRefImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override val isMarkedNullable: Boolean,
    override var receiverTypeRef: FirTypeRef?,
    override val valueParameters: MutableList<FirValueParameter>,
    override var returnTypeRef: FirTypeRef,
    override val isSuspend: Boolean,
    override val contextReceiverTypeRefs: MutableList<FirTypeRef>,
) : FirFunctionTypeRef() {
    override val elementKind get() = FirElementKind.FunctionTypeRef

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {
        receiverTypeRef = newReceiverTypeRef
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceContextReceiverTypeRefs(newContextReceiverTypeRefs: List<FirTypeRef>) {
        contextReceiverTypeRefs.clear()
        contextReceiverTypeRefs.addAll(newContextReceiverTypeRefs)
    }
}
