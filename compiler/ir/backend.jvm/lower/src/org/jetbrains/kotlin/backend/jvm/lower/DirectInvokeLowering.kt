/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Inlines directly invoked lambdas and replaces invoked function references with calls.
 */
internal class DirectInvokeLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitCall(expression: IrCall): IrExpression {
        val function = expression.symbol.owner
        val receiver = expression.dispatchReceiver
        if (receiver == null || function.name != OperatorNameConventions.INVOKE) return super.visitCall(expression)

        val result = if (receiver is IrRichFunctionReference) {
            context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).run {
                lateinit var tempVariablesForArgs: List<IrVariable>
                irBlock {
                    assert(expression.arguments.isNotEmpty()) { "`invoke` call must have an invokable argument" }
                    tempVariablesForArgs = (receiver.boundValues + expression.arguments.drop(1).requireNoNulls()).map(::createTmpVariable)
                    +receiver.invokeFunction.inline(currentDeclarationParent!!, tempVariablesForArgs)
                } // We had to create temporary variables to use IrFunction.inline().
                    // If the arguments were constant, futher optimizations (e.g. FlattenStringConcatenationLowering) might fail to trigger.
                    // (see codegen/box/directInvokeOptimization/unboundMemberRef.kt for example)
                    // Here we optimize such newly created useless variables out.
                    .transform(object : IrTransformer<IrDeclaration?>() {
                        override fun visitGetValue(expression: IrGetValue, data: IrDeclaration?) =
                            optimizeGetValue(expression) { it !in tempVariablesForArgs }

                        override fun visitBlock(expression: IrBlock, data: IrDeclaration?): IrExpression {
                            expression.transformChildren(this, data)
                            removeUnnecessaryTemporaryVariables(expression.statements) { it !in tempVariablesForArgs }
                            return expression
                        }
                    }, null)
            }
        } else {
            expression
        }

        result.transformChildrenVoid()
        return result
    }
}