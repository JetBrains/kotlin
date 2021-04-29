/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed interface FirDeclaration : FirElement {
    override val source: FirSourceElement?
    val moduleData: FirModuleData
    val resolvePhase: FirResolvePhase
    val origin: FirDeclarationOrigin
    val attributes: FirDeclarationAttributes

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformDeclaration(this, data) as E

    fun replaceResolvePhase(newResolvePhase: FirResolvePhase)
}
