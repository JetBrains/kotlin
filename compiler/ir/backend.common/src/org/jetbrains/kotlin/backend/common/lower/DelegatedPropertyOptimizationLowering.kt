/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

private val KPROPERTIES_FOR_DELEGATION by IrDeclarationOriginImpl


class DelegatedPropertyOptimizationLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        var refId = 0
        val newFields = mutableListOf<IrField>()
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            val delegatedProperties = mutableMapOf<IrSymbol, (IrExpression) -> IrExpression>()

            inline fun withLazyFieldFor(declaration: IrDeclaration?, block: () -> Unit) {
                if (declaration != null) {
                    var field : IrField? = null
                    delegatedProperties[declaration.symbol] = fun(expression: IrExpression): IrExpression {
                        if (field == null) {
                            field = declaration.factory.buildField {
                                visibility = DescriptorVisibilities.PRIVATE
                                name = "KPROPERTY${refId++}".synthesizedName
                                isFinal = true
                                isStatic = true
                                type = expression.type
                                origin = KPROPERTIES_FOR_DELEGATION
                            }.apply {
                                initializer = declaration.factory.createExpressionBody(expression.startOffset, expression.endOffset, expression)
                                parent = irFile
                                initializer?.setDeclarationsParent(this)
                                newFields.add(this)
                            }
                        }
                        return IrGetFieldImpl(expression.startOffset, expression.endOffset, field.symbol, expression.type)
                    }
                }
                block()
                if (declaration != null) {
                    delegatedProperties.remove(declaration.symbol)
                }
            }

            override fun visitProperty(declaration: IrProperty): IrProperty {
                context.irFactory.stageController.restrictTo(declaration) {
                    withLazyFieldFor(declaration.takeIf { it.isDelegated }) {
                        declaration.transformChildrenVoid(this)
                    }
                }
                return declaration
            }

            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
                context.irFactory.stageController.restrictTo(declaration) {
                    withLazyFieldFor(declaration) {
                        declaration.transformChildrenVoid(this)
                    }
                }
                return declaration
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                super.visitPropertyReference(expression)
                if (expression.arguments.any { it != null }) return expression
                return delegatedProperties[expression.symbol]?.invoke(expression) ?: expression
            }

            override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
                super.visitLocalDelegatedPropertyReference(expression)
                if (expression.arguments.any { it != null }) return expression
                return delegatedProperties[expression.symbol]?.invoke(expression) ?: expression
            }

            override fun visitRichPropertyReference(expression: IrRichPropertyReference): IrExpression {
                super.visitRichPropertyReference(expression)
                if (expression.boundValues.isNotEmpty()) return expression
                return expression.reflectionTargetSymbol?.let { delegatedProperties[it] }?.invoke(expression) ?: expression
            }
        })
        irFile.declarations.addAll(0, newFields)
    }
}