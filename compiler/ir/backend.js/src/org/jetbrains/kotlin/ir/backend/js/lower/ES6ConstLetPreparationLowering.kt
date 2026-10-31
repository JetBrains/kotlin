/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.js.config.useEs6ConstLet

@PhasePrerequisites(ForLoopsLowering::class)
internal class ES6ConstLetPreparationLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        if (!context.configuration.useEs6ConstLet) return
        super.lower(irModule)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression = hoistVariablesReferencedInCondition(loop) {
                super.visitDoWhileLoop(it)
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                if (declaration.initializer == null) {
                    // 'const' variable must be assigned immediately in JavaScript.
                    declaration.isVar = true
                }
                return super.visitVariable(declaration)
            }
        })
    }

    /**
     * Unlike in Kotlin, in JavaScript the condition of do-while cannot reference const/let variables declared
     * in the loop body, so we have to hoist them out of the loop.
     */
    fun hoistVariablesReferencedInCondition(
        loop: IrDoWhileLoop,
        transformChildren: (IrDoWhileLoop) -> IrExpression = { it },
    ): IrExpression {
        val variablesReferencedInCondition = hashSetOf<IrVariableSymbol>()
        loop.condition.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitGetValue(expression: IrGetValue) {
                (expression.symbol as? IrVariableSymbol)?.let(variablesReferencedInCondition::add)
            }
        })
        if (variablesReferencedInCondition.isEmpty()) {
            return transformChildren(loop)
        } else {
            val variablesToHoist = mutableListOf<IrVariable>()
            loop.body?.transform(object : IrElementTransformerVoid() {
                override fun visitVariable(declaration: IrVariable): IrStatement {
                    if (declaration.symbol !in variablesReferencedInCondition) return super.visitVariable(declaration)
                    variablesToHoist.add(declaration)
                    declaration.isVar = true
                    val initializer = declaration.initializer
                    return if (initializer == null) {
                        JsIrBuilder.buildComposite(context.irBuiltIns.unitType)
                    } else {
                        declaration.initializer = null
                        IrSetValueImpl(
                            declaration.startOffset,
                            declaration.endOffset,
                            context.irBuiltIns.unitType,
                            declaration.symbol,
                            initializer.transform(this, null),
                            null
                        )
                    }
                }
            }, null)
            return JsIrBuilder.buildBlock(context.irBuiltIns.unitType, variablesToHoist + transformChildren(loop))
        }
    }
}
