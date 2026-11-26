/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Removes property references that an inline callee never reads.
 *
 * Runs before first-stage inlining, while the original calls and their property-reference arguments are still available.
 * For example, Lazy.getValue only reads the lazy value and does not need the property reference passed by the accessor.
 */
internal class RemoveUnusedDelegatedPropertyReferencesLowering(
    context: PreSerializationLoweringContext
) : IrTransformer<IrDeclaration>(), BodyLoweringPass {

    private val inlineFunctionResolver: InlineFunctionResolver = JsInlineFunctionResolver(
        context,
        inlineMode = InlineMode.ALL_INLINE_FUNCTIONS
    )

    private val deserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(context.irMangler)
    )

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.accept(this, container)
    }

    override fun visitFunctionAccess(
        expression: IrFunctionAccessExpression,
        data: IrDeclaration
    ): IrElement {
        expression.transformChildren(this, data)

        if (expression.arguments.none {
                it is IrRichPropertyReference && it.origin == IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE
            }) return expression

        // Ordinary calls must retain their arguments; only inline callees are considered.
        val function = resolveInlineFunction(expression) ?: return expression
        val body = function.body ?: return expression

        for ([index, argument] in expression.arguments.withIndex()) {
            if (argument !is IrRichPropertyReference) continue
            if (argument.origin != PROPERTY_REFERENCE_FOR_DELEGATE) continue
            // Evaluating a bound receiver can have side effects, even when the reference is unused.
            if (argument.boundValues.isNotEmpty()) continue

            val parameter = function.parameters[index]
            if (body.usesParameter(parameter)) continue

            expression.arguments[index] = IrConstImpl.constNull(
                argument.startOffset,
                argument.endOffset,
                argument.type,
            )
        }

        return expression
    }

    /** Loads an external inline body from its KLIB when the resolved declaration has no body. */
    private fun resolveInlineFunction(expression: IrFunctionAccessExpression): IrFunction? {
        val function = inlineFunctionResolver.getFunctionDeclarationToInline(expression)

        return when {
            function == null -> null
            function.body != null -> function
            function is IrSimpleFunction ->
                deserializer.deserializeInlineFunction(function)
            else -> null
        }
    }
}

/** Includes nested declarations so that uses captured by lambdas and local functions are preserved. */
private fun IrBody.usesParameter(parameter: IrValueParameter): Boolean {
    var propertyUsed = false

    acceptChildrenVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (!propertyUsed) {
                element.acceptChildrenVoid(this)
            }
        }

        override fun visitGetValue(expression: IrGetValue) {
            if (expression.symbol == parameter.symbol) {
                propertyUsed = true
            }
        }
    })

    return propertyUsed
}
