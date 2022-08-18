/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.types.FirPlaceholderProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirPlaceholderProjectionImpl(
    override val source: KtSourceElement?,
) : FirPlaceholderProjection() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPlaceholderProjectionImpl {
        return this
    }
}
