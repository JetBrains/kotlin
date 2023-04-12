/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

interface ConeContractDescriptionElement {
    fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R

    val erroneous: Boolean
}

abstract class ConeEffectDeclaration : ConeContractDescriptionElement {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitEffectDeclaration(this, data)
}

interface ConeBooleanExpression : ConeContractDescriptionElement {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitBooleanExpression(this, data)
}
