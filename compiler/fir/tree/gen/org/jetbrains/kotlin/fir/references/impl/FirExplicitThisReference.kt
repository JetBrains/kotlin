/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.references.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirExplicitThisReference(
    override val source: KtSourceElement?,
    override val labelName: String?,
    override var contextReceiverNumber: Int,
    override var diagnostic: ConeDiagnostic?,
) : FirThisReference() {
    override var boundSymbol: FirBasedSymbol<*>? = null
    override val isImplicit: Boolean = false

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirExplicitThisReference {
        return this
    }

    override fun replaceBoundSymbol(newBoundSymbol: FirBasedSymbol<*>?) {
        boundSymbol = newBoundSymbol
    }

    override fun replaceContextReceiverNumber(newContextReceiverNumber: Int) {
        contextReceiverNumber = newContextReceiverNumber
    }

    override fun replaceDiagnostic(newDiagnostic: ConeDiagnostic?) {
        diagnostic = newDiagnostic
    }
}
