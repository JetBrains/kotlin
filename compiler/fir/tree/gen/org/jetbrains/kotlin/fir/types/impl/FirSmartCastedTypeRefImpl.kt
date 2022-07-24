/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirSmartCastedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirSmartCastedTypeRefImpl @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override var delegatedTypeRef: FirTypeRef?,
    override val isFromStubType: Boolean,
    override val typesFromSmartCast: Collection<ConeKotlinType>,
    override val originalType: ConeKotlinType,
    override val smartcastType: ConeKotlinType,
    override val smartcastStability: SmartcastStability,
) : FirSmartCastedTypeRef() {
    override val type: ConeKotlinType get() = if (isStable) smartcastType else originalType
    override val isStable: Boolean get() = smartcastStability == SmartcastStability.STABLE_VALUE

    override val elementKind get() = FirElementKind.SmartCastedTypeRef

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceDelegatedTypeRef(newDelegatedTypeRef: FirTypeRef?) {
        delegatedTypeRef = newDelegatedTypeRef
    }
}
