/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.enclosingMethodOverride
import org.jetbrains.kotlin.backend.jvm.ir.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.ir.unwrapInlineLambda
import org.jetbrains.kotlin.backend.jvm.ir.unwrapRichInlineLambda
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor

/**
 * Finds enclosing methods for objects inside inline and dynamic lambdas.
 */
internal class RecordEnclosingMethodsLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) =
        irFile.accept(object : IrVisitor<Unit, IrFunction?>() {
            private val classStack = mutableListOf<IrClass>()

            override fun visitClass(declaration: IrClass, data: IrFunction?) {
                classStack.add(declaration)
                super.visitClass(declaration, data)
                classStack.removeLast()
            }

            override fun visitElement(element: IrElement, data: IrFunction?) =
                element.acceptChildren(this, element as? IrFunction ?: data)

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrFunction?) {
                require(data != null) { "function call not in a method: ${expression.render()}" }
                when {
                    expression.symbol == context.symbols.indyLambdaMetafactoryIntrinsic -> {
                        val reference = expression.arguments[1]
                        // TODO remove after KT-78719
                        if (reference is IrFunctionReference && reference.origin.isLambda) {
                            recordEnclosingMethodOverride(reference.symbol.owner, data)
                        }
                        if (reference is IrRichFunctionReference && reference.origin.isLambda) {
                            recordEnclosingMethodOverride(reference.invokeFunction, data)
                        }
                    }
                    expression.symbol.owner.isInlineFunctionCall(context) -> {
                        for (parameter in expression.symbol.owner.parameters) {
                            // TODO remove after KT-78719
                            expression.arguments[parameter]?.unwrapInlineLambda()
                                ?.let { recordEnclosingMethodOverride(it.symbol.owner, data) }
                            expression.arguments[parameter]?.unwrapRichInlineLambda()
                                ?.let { recordEnclosingMethodOverride(it.invokeFunction, data) }
                        }
                    }
                }
                return super.visitFunctionAccess(expression, data)
            }

            private fun recordEnclosingMethodOverride(from: IrFunction, to: IrFunction) {
                val old = from.enclosingMethodOverride
                if (old != null) {
                    // A single lambda can be referenced multiple times if it is in a field initializer
                    // or an anonymous initializer block and there are multiple non-delegating constructors.
                    classStack.last().primaryConstructor?.let {
                        from.enclosingMethodOverride = it
                    }
                } else {
                    from.enclosingMethodOverride = to
                }
            }
        }, null)
}
