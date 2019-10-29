/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

abstract class FirAbstractTreeTransformer<D>(phase: FirResolvePhase) : FirAbstractPhaseTransformer<D>(phase) {
    override fun <E : FirElement> transformElement(element: E, data: D): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }
}