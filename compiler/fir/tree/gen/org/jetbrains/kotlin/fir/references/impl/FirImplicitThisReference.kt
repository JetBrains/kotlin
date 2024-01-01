/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.references.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal class FirImplicitThisReference(
    override val boundSymbol: FirBasedSymbol<*>?,
    override var contextReceiverNumber: Int,
    override var diagnostic: ConeDiagnostic?,
) : FirThisReference() {
    override val source: KtSourceElement?
        get() = null
    override val labelName: String?
        get() = null
    override val isImplicit: Boolean = true

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirImplicitThisReference {
        return this
    }

    override fun replaceBoundSymbol(newBoundSymbol: FirBasedSymbol<*>?) {}

    override fun replaceContextReceiverNumber(newContextReceiverNumber: Int) {
        contextReceiverNumber = newContextReceiverNumber
    }

    override fun replaceDiagnostic(newDiagnostic: ConeDiagnostic?) {
        diagnostic = newDiagnostic
    }
}
