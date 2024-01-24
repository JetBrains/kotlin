/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.LocalClassPopupLowering
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.isAdaptedFunctionReference
import org.jetbrains.kotlin.ir.util.isInlineParameter
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.visitors.*

/*
 Here we're extracting some local classes from inline bodies.
 The mental model of inlining is as following:
  - for inline lambdas, since we don't see the keyword `inline` at a callsite,
    it is logical to think that the lambda won't be copied but will be embedded as is at the callsite,
    so all local classes declared in those inline lambdas are NEVER COPIED.
  - as for the bodies of inline functions, then it is the opposite - we see the `inline` keyword,
    so it is only logical to think that this is a macro substitution, so the bodies of inline functions
    are copied. But the compiler could optimize the usage of some local classes and not copy them.
    So in this case all local classes MIGHT BE COPIED.
 */

class LocalClassesInInlineLambdasLowering(val context: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(object : IrElementTransformer<IrDeclarationParent> {
            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent) =
                super.visitDeclaration(declaration, (declaration as? IrDeclarationParent) ?: data)

            override fun visitCall(expression: IrCall, data: IrDeclarationParent): IrElement {
                val rootCallee = expression.symbol.owner
                if (!rootCallee.isInline)
                    return super.visitCall(expression, data)

                expression.extensionReceiver = expression.extensionReceiver?.transform(this, data)
                expression.dispatchReceiver = expression.dispatchReceiver?.transform(this, data)
                val inlineLambdas = mutableListOf<IrFunction>()
                for (index in 0 until expression.valueArgumentsCount) {
                    val argument = expression.getValueArgument(index)
                    val inlineLambda = (argument as? IrFunctionExpression)?.function
                        ?.takeIf { rootCallee.valueParameters[index].isInlineParameter() }
                    if (inlineLambda == null)
                        expression.putValueArgument(index, argument?.transform(this, data))
                    else
                        inlineLambdas.add(inlineLambda)
                }

                val localClasses = mutableSetOf<IrClass>()
                val localFunctions = mutableSetOf<IrFunction>()
                val adaptedFunctions = mutableSetOf<IrSimpleFunction>()
                val transformer = this
                for (lambda in inlineLambdas) {
                    lambda.acceptChildrenVoid(object : IrElementVisitorVoid {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitClass(declaration: IrClass) {
                            declaration.transformChildren(transformer, declaration)

                            localClasses.add(declaration)
                        }

                        override fun visitFunctionExpression(expression: IrFunctionExpression) {
                            expression.function.acceptChildrenVoid(this)
                        }

                        override fun visitFunction(declaration: IrFunction) {
                            declaration.transformChildren(transformer, declaration)

                            localFunctions.add(declaration)
                        }

                        override fun visitCall(expression: IrCall) {
                            val callee = expression.symbol.owner
                            if (!callee.isInline) {
                                expression.acceptChildrenVoid(this)
                                return
                            }

                            expression.extensionReceiver?.acceptVoid(this)
                            expression.dispatchReceiver?.acceptVoid(this)
                            (0 until expression.valueArgumentsCount).forEach { index ->
                                val argument = expression.getValueArgument(index)
                                val parameter = callee.valueParameters[index]
                                // Skip adapted function references - they will be inlined later.
                                if (parameter.isInlineParameter() && argument?.isAdaptedFunctionReference() == true)
                                    adaptedFunctions += (argument as IrBlock).statements[0] as IrSimpleFunction
                                else
                                    argument?.acceptVoid(this)
                            }
                        }
                    })
                }

                if (localClasses.isEmpty() && localFunctions.isEmpty())
                    return expression

                val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, expression.type).apply {
                    statements += expression
                }
                LocalDeclarationsLowering(context).lower(irBlock, container, data, localClasses, adaptedFunctions)
                irBlock.statements.addAll(0, localClasses)

                for (lambda in inlineLambdas) {
                    lambda.transformChildrenVoid(object : IrElementTransformerVoid() {
                        override fun visitClass(declaration: IrClass): IrStatement {
                            return IrCompositeImpl(
                                declaration.startOffset, declaration.endOffset,
                                context.irBuiltIns.unitType
                            )
                        }
                    })
                }
                localClasses.forEach { it.setDeclarationsParent(data) }

                return irBlock
            }
        }, container as? IrDeclarationParent ?: container.parent)
    }
}

class LocalClassesInInlineFunctionsLowering(val context: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val function = container as? IrFunction ?: return
        if (!function.isInline) return
        // Conservatively assume that functions with reified type parameters must be copied.
        if (function.typeParameters.any { it.isReified }) return

        val crossinlineParameters = function.valueParameters.filter { it.isCrossinline }.toSet()
        val classesToExtract = mutableSetOf<IrClass>()
        function.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                var canExtract = true
                if (crossinlineParameters.isNotEmpty()) {
                    declaration.acceptVoid(object : IrElementVisitorVoid {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitGetValue(expression: IrGetValue) {
                            if (expression.symbol.owner in crossinlineParameters)
                                canExtract = false
                        }
                    })
                }
                if (canExtract)
                    classesToExtract.add(declaration)
            }
        })
        if (classesToExtract.isEmpty())
            return

        LocalDeclarationsLowering(context).lower(function, function, classesToExtract)
    }
}

class LocalClassesExtractionFromInlineFunctionsLowering(
    context: CommonBackendContext,
    recordExtractedLocalClasses: BackendContext.(IrClass) -> Unit = {},
) : LocalClassPopupLowering(context, recordExtractedLocalClasses) {
    private val classesToExtract = mutableSetOf<IrClass>()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val function = container as? IrFunction ?: return
        if (!function.isInline) return
        // Conservatively assume that functions with reified type parameters must be copied.
        if (function.typeParameters.any { it.isReified }) return

        val crossinlineParameters = function.valueParameters.filter { it.isCrossinline }.toSet()

        function.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                var canExtract = true
                if (crossinlineParameters.isNotEmpty()) {
                    declaration.acceptVoid(object : IrElementVisitorVoid {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitGetValue(expression: IrGetValue) {
                            if (expression.symbol.owner in crossinlineParameters)
                                canExtract = false
                        }
                    })
                }
                if (canExtract)
                    classesToExtract.add(declaration)
            }
        })
        if (classesToExtract.isEmpty())
            return

        super.lower(irBody, container)

        classesToExtract.clear()
    }

    override fun shouldPopUp(klass: IrClass, currentScope: ScopeWithIr?): Boolean {
        return classesToExtract.contains(klass)
    }
}