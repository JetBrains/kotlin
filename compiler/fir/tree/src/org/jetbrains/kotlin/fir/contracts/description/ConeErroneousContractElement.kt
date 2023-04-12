/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic

class ConeErroneousContractElement(
    val diagnostic: ConeDiagnostic
) : ConeEffectDeclaration(), ConeBooleanExpression, ConeContractDescriptionValue {
    override val erroneous: Boolean
        get() = true

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R {
        return contractDescriptionVisitor.visitErroneousElement(this, data)
    }
}
