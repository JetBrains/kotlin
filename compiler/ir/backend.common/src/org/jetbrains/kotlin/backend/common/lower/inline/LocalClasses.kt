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
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.util.isInlineParameter
import org.jetbrains.kotlin.ir.util.isOriginallyLocalDeclaration
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.visitors.*

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

                if (inlineLambdas.isEmpty())
                    return expression

                // TODO: Remove fragment below after fixing KT-77103
                // This fragment will make sure that local delegated properties accessors are lifted iff there are some other local declarations that could potentially "expose" them.
                // That was a behavior of this lowering before the refactor. Local delegated properties accessors were not lifted if there weren't any other "liftable" declarations in inline lambda.
                // It is just a temporary measure to avoid muting many tests that started reproducing problem explained in KT-77103 After handling this problem it will be
                // possible to completely remove it, since more "relaxed" way of lifting local delegated properties accessors will not have any other negative effects other the one mentioned here.
                val localDeclarations = mutableSetOf<IrDeclaration>()

                for (lambda in inlineLambdas) {
                    lambda.acceptChildrenVoid(object : IrVisitorVoid() {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitClass(declaration: IrClass) {
                            localDeclarations.add(declaration)
                        }

                        override fun visitFunction(declaration: IrFunction) {
                            localDeclarations.add(declaration)
                        }

                        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
                            return
                        }
                    })
                }

                if (localDeclarations.isEmpty())
                    return expression
                // TODO: Remove fragment above after fixing KT-77103

                val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, expression.type).apply {
                    statements += expression
                }
                LocalDeclarationsLowering(
                    context,
                    visibilityPolicy = object : VisibilityPolicy {
                        /**
                         * Local classes extracted from inline lambdas are not yet lifted, so their visibility should remain local.
                         * They will be visited for the second time _after_ function inlining, and only then will they be lifted to
                         * the nearest declaration container by [LocalDeclarationPopupLowering],
                         * so that's when we will change their visibility to private.
                         */
                        override fun forClass(declaration: IrClass, inInlineFunctionScope: Boolean): DescriptorVisibility =
                            declaration.visibility
                    },
                    // Lambdas cannot introduce new type parameters to the scope, which means that all the captured type parameters
                    // are also present in the inline lambda's parent declaration,
                    // which we will extract the local class to.
                    remapCapturedTypesInExtractedLocalDeclarations = false,
                ).lower(irBlock = irBlock, container = container, closestParent = data)

                val localDeclarationsToPopUp = mutableListOf<IrDeclaration>()

                val outerTransformer = this
                for (lambda in inlineLambdas) {
                    lambda.transformChildrenVoid(object : IrElementTransformerVoid() {
                        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
                            declaration.getter.transformStatement(this)
                            declaration.setter?.transformStatement(this)
                            return declaration.delegate.transformStatement(this)
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

                irBlock.statements.addAll(0, localDeclarationsToPopUp)
                localDeclarationsToPopUp.forEach { it.setDeclarationsParent(data) }

                return irBlock
            }
        }, container as? IrDeclarationParent ?: container.parent)
    }
}
