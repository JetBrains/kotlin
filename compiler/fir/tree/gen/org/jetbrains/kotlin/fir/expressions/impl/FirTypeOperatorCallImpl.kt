/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirTypeOperatorCallImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override var argumentList: FirArgumentList,
    override val operation: FirOperation,
    override var conversionTypeRef: FirTypeRef,
) : FirTypeOperatorCall() {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override val elementKind get() = FirElementKind.TypeOperatorCall

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceArgumentList(newArgumentList: FirArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceConversionTypeRef(newConversionTypeRef: FirTypeRef) {
        conversionTypeRef = newConversionTypeRef
    }
}
