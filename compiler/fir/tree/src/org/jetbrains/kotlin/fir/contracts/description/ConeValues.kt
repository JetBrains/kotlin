/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

interface ConeContractDescriptionValue : ConeContractDescriptionElement {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitValue(this, data)
}

open class ConeConstantReference protected constructor(val name: String) : ConeContractDescriptionValue {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitConstantDescriptor(this, data)

    companion object {
        val NULL = ConeConstantReference("NULL")
        val WILDCARD = ConeConstantReference("WILDCARD")
        val NOT_NULL = ConeConstantReference("NOT_NULL")
    }
}

class ConeBooleanConstantReference private constructor(name: String) : ConeConstantReference(name), ConeBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitBooleanConstantDescriptor(this, data)

    companion object {
        val TRUE = ConeBooleanConstantReference("TRUE")
        val FALSE = ConeBooleanConstantReference("FALSE")
    }
}

interface ConeTargetReference : ConeContractDescriptionElement

/*
 * Index of value parameter of function
 * -1 means that it is reference to extension receiver
 */
open class ConeValueParameterReference(val parameterIndex: Int, val name: String) : ConeContractDescriptionValue, ConeTargetReference {
    init {
        assert(parameterIndex >= -1)
    }

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitValueParameterReference(this, data)
}

class ConeBooleanValueParameterReference(parameterIndex: Int, name: String) : ConeValueParameterReference(parameterIndex, name),
    ConeBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitBooleanValueParameterReference(this, data)
}

class ConeLambdaArgumentReference(val lambda: ConeValueParameterReference, val parameter: ConeValueParameterReference) :
    ConeTargetReference {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitLambdaArgumentReference(this, data)
}

abstract class ConeActionDeclaration(
    val target: ConeTargetReference,
    val targetClass: FirRegularClassSymbol,
    val kind: EventOccurrencesRange
) : ConeContractDescriptionElement

class ConePropertyInitializationAction(
    target: ConeTargetReference,
    targetClass: FirRegularClassSymbol,
    val property: FirPropertySymbol,
    kind: EventOccurrencesRange
) : ConeActionDeclaration(target, targetClass, kind) {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitPropertyInitializationAction(this, data)
}

class ConeFunctionInvocationAction(
    target: ConeTargetReference,
    targetClass: FirRegularClassSymbol,
    val function: FirFunctionSymbol<*>,
    kind: EventOccurrencesRange
) : ConeActionDeclaration(target, targetClass, kind) {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitFunctionInvocationAction(this, data)
}