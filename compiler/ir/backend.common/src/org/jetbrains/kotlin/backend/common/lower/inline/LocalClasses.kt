/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.isInlineLambdaBlock
import org.jetbrains.kotlin.backend.common.lower.LocalClassPopupLowering
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.VisibilityPolicy
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.isAdaptedFunctionReference
import org.jetbrains.kotlin.ir.util.isInlineParameter
import org.jetbrains.kotlin.ir.util.isOriginallyLocalDeclaration
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.utils.mapToSetOrEmpty

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
@PhaseDescription("LocalClassesInInlineLambdasLowering")
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

                val inlineLambdas = mutableListOf<IrFunction>()
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
                        inlineLambdas.add(inlineLambda)
                }

                val localClasses = mutableSetOf<IrClass>()
                val localFunctions = mutableSetOf<IrFunction>()
                val adaptedFunctions = mutableSetOf<IrSimpleFunction>()
                val transformer = this
                for (lambda in inlineLambdas) {
                    lambda.acceptChildrenVoid(object : IrVisitorVoid() {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitClass(declaration: IrClass) {
                            declaration.transformChildren(transformer, declaration)

                            localClasses.add(declaration)
                        }

                        override fun visitRichFunctionReference(expression: IrRichFunctionReference) {
                            expression.boundValues.forEach { it.acceptVoid(this)  }
                            expression.invokeFunction.acceptChildrenVoid(this)
                        }

                        override fun visitRichPropertyReference(expression: IrRichPropertyReference) {
                            expression.boundValues.forEach { it.acceptVoid(this)  }
                            expression.getterFunction.acceptChildrenVoid(this)
                            expression.setterFunction?.acceptChildrenVoid(this)
                        }

                        override fun visitFunction(declaration: IrFunction) {
                            declaration.transformChildren(transformer, declaration)

                            localFunctions.add(declaration)
                        }

                        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
                            // Do not extract local delegates from the inline function.
                            // Doing that can lead to inconsistent IR. Local delegated property consists of two elements: property and
                            // accessors to it. Inside the accessor we have a reference to the property.
                            // `LocalClassesInInlineLambdasLowering` can only extract the accessor out of inline lambda, leaving the
                            // property in place. Because of this, we have not entirely correct reference.
                            return
                        }

                        override fun visitCall(expression: IrCall) {
                            val callee = expression.symbol.owner
                            if (!callee.isInline) {
                                expression.acceptChildrenVoid(this)
                                return
                            }

                            expression.arguments.zip(callee.parameters).forEach { (argument, parameter) ->
                                // Skip adapted function references and inline lambdas - they will be inlined later.
                                val shouldSkip = argument != null && (argument.isAdaptedFunctionReference() || argument.isInlineLambdaBlock())
                                if (parameter.isInlineParameter() && shouldSkip)
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
                LocalDeclarationsLowering(
                    context,
                    visibilityPolicy = object : VisibilityPolicy {
                        /**
                         * Local classes extracted from inline lambdas are not yet lifted, so their visibility should remain local.
                         * They will be visited for the second time _after_ function inlining, and only then will they be lifted to
                         * the nearest declaration container by [LocalClassPopupLowering],
                         * so that's when we will change their visibility to private.
                         */
                        override fun forClass(declaration: IrClass, inInlineFunctionScope: Boolean): DescriptorVisibility =
                            declaration.visibility
                    },
                    // Lambdas cannot introduce new type parameters to the scope, which means that all the captured type parameters
                    // are also present in the inline lambda's parent declaration,
                    // which we will extract the local class to.
                    remapCapturedTypesInExtractedLocalDeclarations = false,
                ).lower(irBlock, container, data, localClasses, adaptedFunctions)

                // `LocalDeclarationsLowering` transforms classes in place, but creates new nodes for functions
                val transformedLocalFunctions = mutableSetOf<IrSimpleFunction>()
                // New function nodes share body element with the old ones
                val localFunctionBodies = localFunctions.mapToSetOrEmpty { it.body }
                for (lambda in inlineLambdas) {
                    lambda.acceptChildrenVoid(object : IrVisitorVoid() {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                            // Referential equality of bodies used to avoid replication of traverse logic used in visitor that looked for original localFunctions
                            if (declaration.isOriginallyLocalDeclaration && declaration.body in localFunctionBodies) {
                                transformedLocalFunctions.add(declaration)
                            }
                        }
                    })
                }

                val transformedLocalDeclarations = localClasses + transformedLocalFunctions
                irBlock.statements.addAll(0, transformedLocalDeclarations)
                transformedLocalDeclarations.forEach { it.setDeclarationsParent(data) }

                for (lambda in inlineLambdas) {
                    lambda.transformChildrenVoid(object : IrElementTransformerVoid() {
                        override fun visitClass(declaration: IrClass): IrStatement {
                            return IrCompositeImpl(
                                declaration.startOffset, declaration.endOffset,
                                context.irBuiltIns.unitType
                            )
                        }

                        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                            return if (declaration in transformedLocalFunctions) {
                                IrCompositeImpl(
                                    declaration.startOffset, declaration.endOffset,
                                    context.irBuiltIns.unitType
                                )
                            } else super.visitSimpleFunction(declaration)
                        }
                    })
                }

                return irBlock
            }
        }, container as? IrDeclarationParent ?: container.parent)
    }
}
