/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

object ControlFlowGraphReferenceTransformer : FirTransformer<ControlFlowGraph>() {
    override fun <E : FirElement> transformElement(element: E, data: ControlFlowGraph): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun <F : FirFunction<F>> transformFunction(
        function: FirFunction<F>,
        data: ControlFlowGraph
    ): CompositeTransformResult<FirDeclaration> {
        return (function.transformChildren(this, data) as FirFunction<*>).compose()
    }

    override fun transformControlFlowGraphReference(
        controlFlowGraphReference: FirControlFlowGraphReference,
        data: ControlFlowGraph
    ): CompositeTransformResult<FirControlFlowGraphReference> {
        return if (controlFlowGraphReference is FirEmptyControlFlowGraphReference) {
            FirControlFlowGraphReferenceImpl(data).compose()
        } else {
            controlFlowGraphReference.compose()
        }
    }
}