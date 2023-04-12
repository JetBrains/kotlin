/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic

interface ConeContractDescriptionValue : ConeContractDescriptionElement {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitValue(this, data)
}

open class ConeConstantReference protected constructor(val name: String) : ConeContractDescriptionValue {
    override val erroneous: Boolean
        get() = false

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

class ConeErroneousConstantReference(val diagnostic: ConeDiagnostic) : ConeConstantReference("ERROR") {
    override val erroneous: Boolean
        get() = true

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitErroneousConstantReference(this, data)
}

/*
 * Index of value parameter of function
 * -1 means that it is reference to extension receiver
 */
open class ConeValueParameterReference(val parameterIndex: Int, val name: String) : ConeContractDescriptionValue {
    init {
        assert(parameterIndex >= -1)
    }

    override val erroneous: Boolean
        get() = false

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitValueParameterReference(this, data)
}

class ConeBooleanValueParameterReference(parameterIndex: Int, name: String) : ConeValueParameterReference(parameterIndex, name), ConeBooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitBooleanValueParameterReference(this, data)
}

class ConeErroneousValueParameterReference(val diagnostic: ConeDiagnostic) : ConeValueParameterReference(Int.MAX_VALUE, "ERROR") {
    override val erroneous: Boolean
        get() = true

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitErroneousValueParameterReference(this, data)
}
