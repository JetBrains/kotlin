/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.isInlineParameter

abstract class CommonInlineCallableReferenceToLambdaPhase(
    context: CommonBackendContext,
    inlineFunctionResolver: InlineFunctionResolver
) : InlineCallableReferenceToLambdaPhase(context, inlineFunctionResolver) {
    fun lower(function: IrFunction) = function.accept(this, function.parent)

    override fun visitFunction(declaration: IrFunction, data: IrDeclarationParent?) {
        super.visitFunction(declaration, data)
        if (inlineFunctionResolver.needsInlining(declaration)) {
            for (parameter in declaration.valueParameters) {
                if (parameter.isInlineParameter()) {
                    val defaultExpression = parameter.defaultValue?.expression ?: continue
                    parameter.defaultValue?.expression = defaultExpression.transform(declaration)
                }
            }
        }
    }
}