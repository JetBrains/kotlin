/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirControlFlowGraphOwner : FirElement {
    override val source: FirSourceElement?
    val controlFlowGraphReference: FirControlFlowGraphReference

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitControlFlowGraphOwner(this, data)

    fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirControlFlowGraphOwner
}
