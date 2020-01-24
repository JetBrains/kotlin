/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirDeclaration : FirElement {
    override val source: FirSourceElement?
    val session: FirSession
    val resolvePhase: FirResolvePhase

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitDeclaration(this, data)

    fun replaceResolvePhase(newResolvePhase: FirResolvePhase)
}
