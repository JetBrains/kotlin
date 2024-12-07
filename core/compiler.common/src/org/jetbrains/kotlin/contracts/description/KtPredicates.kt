/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.description

open class KtIsInstancePredicate<Type, Diagnostic>(val arg: KtValueParameterReference<Type, Diagnostic>, val type: Type, val isNegated: Boolean) :
    KtBooleanExpression<Type, Diagnostic> {
    override val erroneous: Boolean
        get() = arg.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitIsInstancePredicate(this, data)

    fun negated(): KtIsInstancePredicate<Type, Diagnostic> =
        KtIsInstancePredicate(arg, type, isNegated.not())
}

class KtErroneousIsInstancePredicate<Type, Diagnostic>(
    arg: KtValueParameterReference<Type, Diagnostic>,
    type: Type,
    isNegated: Boolean,
    val diagnostic: Diagnostic
) : KtIsInstancePredicate<Type, Diagnostic>(arg, type, isNegated) {
    override val erroneous: Boolean
        get() = true

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitErroneousIsInstancePredicate(this, data)
}

class KtIsNullPredicate<Type, Diagnostic>(val arg: KtValueParameterReference<Type, Diagnostic>, val isNegated: Boolean) :
    KtBooleanExpression<Type, Diagnostic> {
    override val erroneous: Boolean
        get() = arg.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitIsNullPredicate(this, data)

    fun negated(): KtIsNullPredicate<Type, Diagnostic> =
        KtIsNullPredicate(arg, isNegated.not())
}
