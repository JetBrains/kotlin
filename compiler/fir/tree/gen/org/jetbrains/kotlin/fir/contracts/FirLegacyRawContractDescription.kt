/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.contracts

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.legacyRawContractDescription]
 */
abstract class FirLegacyRawContractDescription : FirContractDescription() {
    abstract override val source: KtSourceElement?
    abstract val contractCall: FirFunctionCall
    abstract val diagnostic: ConeDiagnostic?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitLegacyRawContractDescription(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformLegacyRawContractDescription(this, data) as E
}
