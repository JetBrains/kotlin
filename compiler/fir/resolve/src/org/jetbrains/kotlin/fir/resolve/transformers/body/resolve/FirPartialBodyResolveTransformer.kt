/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult

abstract class FirPartialBodyResolveTransformer(
    val transformer: FirBodyResolveTransformer
) : FirAbstractBodyResolveTransformer(transformer.transformerPhase) {
    @Suppress("OVERRIDE_BY_INLINE")
    final override inline val components: BodyResolveTransformerComponents get() = transformer.components

    override var implicitTypeOnly: Boolean
        get() = transformer.implicitTypeOnly
        set(value) {
            transformer.implicitTypeOnly = value
        }

    override fun <E : FirElement> transformElement(element: E, data: ResolutionMode): CompositeTransformResult<E> {
        return element.transform(transformer, data)
    }
}