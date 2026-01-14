/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirIdeOnly
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.qualifierWithContextSensitiveAlternative]
 */
interface FirQualifierWithContextSensitiveAlternative : FirElement {
    override val source: KtSourceElement?
    /**
     * For resolved qualifier, it contains either null or a simple name property access which would be used for checking
     * if context-sensitive resolution might be used instead of the owner qualifier. 
     * For example, if the owner is `MyEnum.X`, then contextSensitiveAlternative would be just `X`.
     *
     * Only used in ideMode to find out if the property access can be replaced with a simple name expression
     * via context-sensitive resolution, so the reference shortener/inspections might use this information.
     *
     * Even in ideMode, it's only initialized if there is a reason to assume that it might be the case of CSR, e.g., 
     * it should be left `null` for ContextIndependent resolution mode.
     */
    @FirIdeOnly
    val contextSensitiveAlternative: FirPropertyAccessExpression?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitQualifierWithContextSensitiveAlternative(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformQualifierWithContextSensitiveAlternative(this, data) as E

    fun replaceContextSensitiveAlternative(newContextSensitiveAlternative: FirPropertyAccessExpression?)
}
