/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionValue]
 */
public sealed interface KtContractDescriptionValue : KtContractDescriptionElement {
    public override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitValue(this, data)
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.ConstantReference]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference]
 */
public open class KtConstantReference(public val name: String) : KtContractDescriptionValue {
    public override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitConstantDescriptor(this, data)

    public companion object {
        public val NULL: KtConstantReference = KtConstantReference("NULL")
        public val WILDCARD: KtConstantReference = KtConstantReference("WILDCARD")
        public val NOT_NULL: KtConstantReference = KtConstantReference("NOT_NULL")
    }
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.BooleanConstantReference]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference]
 */
public class KtBooleanConstantReference(name: String) : KtConstantReference(name), KtBooleanExpression {
    public override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitBooleanConstantDescriptor(this, data)

    public companion object {
        public val TRUE: KtBooleanConstantReference = KtBooleanConstantReference("TRUE")
        public val FALSE: KtBooleanConstantReference = KtBooleanConstantReference("FALSE")
    }
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.VariableReference]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeValueParameterReference]
 */
public open class KtValueParameterReference(public val name: String) : KtContractDescriptionValue {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitValueParameterReference(this, data)
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.BooleanVariableReference]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanValueParameterReference]
 */
public class KtBooleanValueParameterReference(name: String) : KtValueParameterReference(name), KtBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitBooleanValueParameterReference(this, data)
}
