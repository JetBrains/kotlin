/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirAnonymousInitializerImpl(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override var body: FirBlock?
) : FirAnonymousInitializer() {
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAnonymousInitializerImpl {
        body = body?.transformSingle(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }
}
