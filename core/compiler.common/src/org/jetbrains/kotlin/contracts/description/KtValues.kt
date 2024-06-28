/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.description

interface KtContractDescriptionValue<Type, Diagnostic> : KtContractDescriptionElement<Type, Diagnostic> {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitValue(this, data)
}

open class KtConstantReference<Type, Diagnostic>(val name: String) : KtContractDescriptionValue<Type, Diagnostic> {
    override val erroneous: Boolean
        get() = false

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitConstantDescriptor(this, data)
}

class KtBooleanConstantReference<Type, Diagnostic>(name: String) : KtConstantReference<Type, Diagnostic>(name),
    KtBooleanExpression<Type, Diagnostic> {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitBooleanConstantDescriptor(this, data)
}

class KtErroneousConstantReference<Type, Diagnostic>(val diagnostic: Diagnostic) : KtConstantReference<Type, Diagnostic>("ERROR") {
    override val erroneous: Boolean
        get() = true

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitErroneousConstantReference(this, data)
}

/*
 * Index of value parameter of function
 * -1 means that it is reference to extension receiver
 */
open class KtValueParameterReference<Type, Diagnostic>(val parameterIndex: Int, val name: String) :
    KtContractDescriptionValue<Type, Diagnostic> {
    init {
        assert(parameterIndex >= -1)
    }

    override val erroneous: Boolean
        get() = false

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitValueParameterReference(this, data)
}

class KtBooleanValueParameterReference<Type, Diagnostic>(parameterIndex: Int, name: String) : KtValueParameterReference<Type, Diagnostic>(parameterIndex, name),
    KtBooleanExpression<Type, Diagnostic> {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitBooleanValueParameterReference(this, data)
}

class KtErroneousValueParameterReference<Type, Diagnostic>(val diagnostic: Diagnostic) : KtValueParameterReference<Type, Diagnostic>(Int.MAX_VALUE, "ERROR") {
    override val erroneous: Boolean
        get() = true

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitErroneousValueParameterReference(this, data)
}
