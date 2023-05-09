/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.description

class KtErroneousContractElement<Type, Diagnostic>(
    val diagnostic: Diagnostic
) : KtEffectDeclaration<Type, Diagnostic>(), KtBooleanExpression<Type, Diagnostic>, KtContractDescriptionValue<Type, Diagnostic> {
    override val erroneous: Boolean
        get() = true

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R {
        return contractDescriptionVisitor.visitErroneousElement(this, data)
    }
}
