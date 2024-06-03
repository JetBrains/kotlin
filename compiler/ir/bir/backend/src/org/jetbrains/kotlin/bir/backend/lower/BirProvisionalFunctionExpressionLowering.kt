/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.builders.build
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.expressions.impl.BirFunctionReferenceImpl
import org.jetbrains.kotlin.bir.resetWithNulls

import org.jetbrains.kotlin.bir.util.ancestors
import org.jetbrains.kotlin.bir.util.copyAttributes
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

context(JvmBirBackendContext)
class BirProvisionalFunctionExpressionLowering : BirLoweringPhase() {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun lower(module: BirModuleFragment) {
        getAllElementsOfClass(BirFunctionExpression, false).forEach { expression ->
            replaceFunctionExpression(expression)
        }
    }

    private fun replaceFunctionExpression(expression: BirFunctionExpression) {
        var sourceSpan = expression.sourceSpan
        for (element in expression.ancestors()) {
            if (element is BirVariable || element is BirCall) {
                sourceSpan = element.sourceSpan
                break
            } else if (element is BirBody || element is BirReturnableBlock || element is BirFunctionExpression) {
                break
            }
        }

        val function = expression.function
        val block = BirBlock.build {
            this.sourceSpan = sourceSpan
            type = expression.type
            origin = expression.origin

            statements += function
            statements += BirFunctionReferenceImpl(
                sourceSpan = sourceSpan,
                type = expression.type,
                symbol = function.symbol,
                dispatchReceiver = null,
                extensionReceiver = null,
                origin = expression.origin,
                typeArguments = emptyList(),
                reflectionTarget = null,
            ).apply {
                valueArguments.resetWithNulls(function.valueParameters.size)
                copyAttributes(expression)
            }
        }
        expression.replaceWith(block)
    }
}