/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirImplicitTypeRefImpl(
    override val source: KtSourceElement?,
) : FirImplicitTypeRef() {
    override val annotations: List<FirAnnotation> get() = emptyList()

    override val elementKind get() = FirElementKind.ImplicitTypeRef

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {}
}
