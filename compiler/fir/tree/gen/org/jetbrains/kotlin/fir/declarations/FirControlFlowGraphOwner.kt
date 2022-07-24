/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirControlFlowGraphOwner : FirElement {
    override val source: KtSourceElement?
    val controlFlowGraphReference: FirControlFlowGraphReference?


    fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)
}

inline fun <D> FirControlFlowGraphOwner.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirControlFlowGraphOwner 
     = apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }
