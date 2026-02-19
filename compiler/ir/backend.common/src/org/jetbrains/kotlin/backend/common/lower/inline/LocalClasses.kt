/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationPopupLowering
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.VisibilityPolicy
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.util.isInlineParameter
import org.jetbrains.kotlin.ir.util.isOriginallyLocalDeclaration
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Extracts local classes from inline lambdas.
 *
 * The mental model of inlining is as following:
 *  - for inline lambdas, since we don't see the keyword `inline` at the call site,
 *    it is logical to think that the lambda won't be copied but will be embedded as is at the call site,
 *    so all local classes declared in those inline lambdas are NEVER COPIED.
 *  - as for the bodies of inline functions, it is the opposite - we see the `inline` keyword,
 *    so it is only logical to think that this is a macro substitution, so the bodies of inline functions
 *    are copied. But the compiler could optimize the usage of some local classes and not copy them.
 *    So in this case all local classes MIGHT BE COPIED.
 */
class LocalClassesInInlineLambdasLowering(val context: LoweringContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(object : IrTransformer<IrDeclarationParent>() {
            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent) =
                super.visitDeclaration(declaration, (declaration as? IrDeclarationParent) ?: data)

            override fun visitCall(expression: IrCall, data: IrDeclarationParent): IrElement {
                val rootCallee = expression.symbol.owner
                if (!rootCallee.isInline)
                    return super.visitCall(expression, data)

                val inlinableLambdas = mutableListOf<IrFunction>()
                for (index in expression.arguments.indices) {
                    val argument = expression.arguments[index]
                    val inlineLambda = when (argument) {
                        is IrRichPropertyReference -> argument.getterFunction
                        is IrRichFunctionReference -> argument.invokeFunction
                        else -> null
                    }?.takeIf { rootCallee.parameters[index].isInlineParameter() }
                    if (inlineLambda == null)
                        expression.arguments[index] = argument?.transform(this, data)
                    else
                        inlinableLambdas.add(inlineLambda)
                }

                if (inlinableLambdas.isEmpty())
                    return expression

                val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, expression.type).apply {
                    statements += expression
                }
                LocalDeclarationsLowering(
                    context,
                    visibilityPolicy = object : VisibilityPolicy {
                        /**
                         * Local classes and local functions extracted from inline lambdas are not yet lifted,
                         * so their visibility should remain unchanged. They will be visited for the second time on the second compilation
                         * stage, and only then will they be lifted to the nearest declaration container by [LocalDeclarationPopupLowering],
                         * so that's when we will change their visibility to private.
                         */
                        override fun forClass(declaration: IrClass, inInlineFunctionScope: Boolean) = declaration.visibility
                        override fun forSimpleFunction(declaration: IrSimpleFunction) = declaration.visibility
                    },
                    // Lambdas cannot introduce new type parameters to the scope, which means that all the captured type parameters
                    // are also present in the inline lambda's parent declaration,
                    // which we will extract the local class to.
                    remapCapturedTypesInExtractedLocalDeclarations = false,
                ).lower(irBlock = irBlock, container = container, closestParent = data)

                val localDeclarationsToPopUp = mutableListOf<IrDeclaration>()

                val outerTransformer = this
                for (lambda in inlinableLambdas) {
                    lambda.transformChildrenVoid(object : IrElementTransformerVoid() {
                        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
                            /*
                             * Keep the `delegate` variable separately from the rest of the local delegated property.
                             * Extract the local delegated property but keep the variable in place.
                             *
                             * Illustrative example:
                             *
                             * // before the transformation:
                             * IrCall {
                             *     arguments[N] = <inlinable-lambda> {
                             *         ...
                             *         IrLocalDelegatedProperty {
                             *             delegate = IrVariableImpl {
                             *                 symbol = X,
                             *                 initializer = ...,
                             *             }
                             *             getter = IrFunctionImpl {
                             *                 body = // reads the state of the variable with the symbol 'X'
                             *             }
                             *             setter = IrFunctionImpl {
                             *                 body = // modifies the state of the variable with the symbol 'X'
                             *             }
                             *         }
                             *         ...
                             *     }
                             * }
                             *
                             * // after the transformation:
                             * IrLocalDelegatedProperty {
                             *     delegate = null
                             *     getter = IrFunctionImpl {
                             *         body = // reads the state of the variable with the symbol 'X'
                             *     }
                             *     setter = IrFunctionImpl {
                             *         body = // modifies the state of the variable with the symbol 'X'
                             *     }
                             * }
                             * ...
                             * IrCall {
                             *     arguments[N] = <inlinable-lambda> {
                             *         ...
                             *         IrVariableImpl {
                             *             symbol = X,
                             *             initializer = ...,
                             *         }
                             *         ...
                             *     }
                             * }
                             */
                            val delegate = declaration.delegate
                                ?: error("Local delegated property ${declaration.render()} has not delegate")

                            declaration.delegate = null
                            localDeclarationsToPopUp += declaration

                            return delegate
                        }

                        override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression {
                            expression.boundValues.forEach { it.transformStatement(this) }
                            expression.invokeFunction.transformChildrenVoid(this)
                            return expression
                        }

                        override fun visitRichPropertyReference(expression: IrRichPropertyReference): IrExpression {
                            expression.boundValues.forEach { it.transformStatement(this) }
                            expression.getterFunction.transformChildrenVoid(this)
                            expression.setterFunction?.transformChildrenVoid(this)
                            return expression
                        }

                        override fun visitClass(declaration: IrClass): IrStatement = visitSimpleFunctionOrClass(declaration)

                        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement =
                            visitSimpleFunctionOrClass(declaration)

                        private fun visitSimpleFunctionOrClass(declaration: IrDeclaration): IrStatement {
                            // Recursive call to outer transformer for handling nested inline lambdas
                            declaration.transformChildren(outerTransformer, declaration as IrDeclarationParent)
                            return if (declaration.isOriginallyLocalDeclaration) {
                                localDeclarationsToPopUp += declaration
                                IrCompositeImpl(
                                    declaration.startOffset, declaration.endOffset,
                                    context.irBuiltIns.unitType
                                )
                            } else declaration
                        }

                    })
                }

                if (localDeclarationsToPopUp.isEmpty())
                    return expression

                irBlock.statements.addAll(0, localDeclarationsToPopUp)
                localDeclarationsToPopUp.forEach { it.setDeclarationsParent(data) }

                return irBlock
            }
        }, container as? IrDeclarationParent ?: container.parent)
    }
}
