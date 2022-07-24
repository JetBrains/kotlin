/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAnonymousInitializer : FirDeclaration(), FirControlFlowGraphOwner {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract val body: FirBlock?
    abstract override val symbol: FirAnonymousInitializerSymbol


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract fun replaceBody(newBody: FirBlock?)
}

inline fun <D> FirAnonymousInitializer.transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousInitializer 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirAnonymousInitializer.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirAnonymousInitializer 
     = apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }

inline fun <D> FirAnonymousInitializer.transformBody(transformer: FirTransformer<D>, data: D): FirAnonymousInitializer 
     = apply { replaceBody(body?.transform(transformer, data)) }
