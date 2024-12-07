/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.types.FirPlaceholderProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal class FirPlaceholderProjectionImpl(
    override val source: KtSourceElement?,
) : FirPlaceholderProjection() {

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPlaceholderProjectionImpl {
        return this
    }
}
