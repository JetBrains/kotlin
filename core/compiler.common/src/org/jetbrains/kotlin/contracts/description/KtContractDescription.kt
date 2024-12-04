/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.description

interface KtContractDescriptionElement<Type, Diagnostic> {
    fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R

    val erroneous: Boolean
}

abstract class KtEffectDeclaration<Type, Diagnostic> : KtContractDescriptionElement<Type, Diagnostic> {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitEffectDeclaration(this, data)
}

interface KtBooleanExpression<Type, Diagnostic> : KtContractDescriptionElement<Type, Diagnostic> {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitBooleanExpression(this, data)
}
