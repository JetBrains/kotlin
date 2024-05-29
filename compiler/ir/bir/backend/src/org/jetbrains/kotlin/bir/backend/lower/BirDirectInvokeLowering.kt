/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.builders.birBlock
import org.jetbrains.kotlin.bir.backend.builders.birBodyScope
import org.jetbrains.kotlin.bir.backend.builders.birCall
import org.jetbrains.kotlin.bir.backend.builders.build
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.utils.asInlinableFunctionReference
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirBlock
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirFunctionAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirFunctionReference
import org.jetbrains.kotlin.bir.expressions.BirReturn
import org.jetbrains.kotlin.bir.expressions.BirReturnableBlock
import org.jetbrains.kotlin.bir.expressions.BirValueAccessExpression
import org.jetbrains.kotlin.bir.expressions.impl.BirReturnableBlockImpl
import org.jetbrains.kotlin.bir.getBackReferences
import org.jetbrains.kotlin.bir.symbols.BirReturnTargetSymbol
import org.jetbrains.kotlin.bir.util.ancestors
import org.jetbrains.kotlin.bir.util.dump
import org.jetbrains.kotlin.bir.util.explicitParameters
import org.jetbrains.kotlin.bir.util.render
import org.jetbrains.kotlin.bir.util.statements
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

context(JvmBirBackendContext)
class BirDirectInvokeLowering : BirLoweringPhase() {
    private val valueAccesses = registerBackReferencesKey_valueSymbol(BirValueAccessExpression, BirValueAccessExpression::symbol)
    private val returnTargets = registerBackReferencesKey_returnTargetSymbol(BirReturn, BirReturn::returnTargetSymbol)
    private val invokeFunctions = registerIndexKey(BirSimpleFunction, false) {
        it.name == OperatorNameConventions.INVOKE
    }
    private val functionCalls = registerBackReferencesKey(BirCall, BirCall::symbol)

    override fun lower(module: BirModuleFragment) {
        getAllElementsWithIndex(invokeFunctions).forEach { function ->
            function.getBackReferences(functionCalls).forEach { call ->
                val receiver = call.dispatchReceiver
                val result = when {
                    // TODO deal with type parameters somehow?
                    // It seems we can't encounter them in the code written by user,
                    // but this might be important later if we actually perform inlining and optimizations on IR.
                    receiver is BirFunctionReference && receiver.symbol.owner.typeParameters.isEmpty() ->
                        visitFunctionReferenceInvoke(call, receiver)
                    receiver is BirBlock ->
                        receiver.asInlinableFunctionReference()
                            ?.takeIf { it.extensionReceiver == null }
                            ?.let { reference -> visitLambdaInvoke(call, reference) }
                            ?: call
                    else -> call
                }

                call.replaceWith(result)
            }
        }
    }

    private fun visitLambdaInvoke(expression: BirCall, reference: BirFunctionReference): BirExpression {
        val function = reference.symbol.owner
        val declarationParent = expression.ancestors().firstIsInstance<BirDeclarationParent>()

        if (expression.valueArguments.isEmpty()) {
            return function.inline(declarationParent)
        } else {
            val arguments = function.explicitParameters.mapIndexed { index, parameter ->
                val argument = expression.valueArguments[index]!!
                BirVariable.build {
                    sourceSpan = argument.sourceSpan
                    origin = IrDeclarationOrigin.DEFINED
                    name = parameter.name
                    type = parameter.type
                    initializer = argument
                }
            }

            return birBodyScope {
                sourceSpan = expression.sourceSpan
                birBlock {
                    +arguments
                    +function.inline(declarationParent, arguments)
                }
            }
        }
    }

    private fun visitFunctionReferenceInvoke(expression: BirCall, receiver: BirFunctionReference): BirExpression =
        when (val irFun = receiver.symbol.owner) {
            is BirSimpleFunction -> birBodyScope {
                sourceSpan = expression.sourceSpan
                birCall(irFun, expression.type) {
                    copyReceiverAndValueArgumentsForDirectInvoke(receiver, expression)
                }
            }
            is BirConstructor -> birBodyScope {
                sourceSpan = expression.sourceSpan
                birCall(irFun, expression.type) {
                    copyReceiverAndValueArgumentsForDirectInvoke(receiver, expression)
                }
            }
            else -> throw AssertionError("Simple function or constructor expected: ${irFun.render()}")
        }

    private fun BirFunctionAccessExpression.copyReceiverAndValueArgumentsForDirectInvoke(
        irFunRef: BirFunctionReference,
        irInvokeCall: BirFunctionAccessExpression,
    ) {
        val irFun = irFunRef.symbol.owner
        var invokeArgIndex = 0
        if (irFun.dispatchReceiverParameter != null) {
            dispatchReceiver = irFunRef.dispatchReceiver ?: irInvokeCall.valueArguments[invokeArgIndex++]
        }
        if (irFun.extensionReceiverParameter != null) {
            extensionReceiver = irFunRef.extensionReceiver ?: irInvokeCall.valueArguments[invokeArgIndex++]
        }
        if (invokeArgIndex + valueArguments.size != irInvokeCall.valueArguments.size) {
            throw AssertionError("Mismatching value arguments: $invokeArgIndex arguments used for receivers\n${irInvokeCall.dump()}")
        }
        for (i in 0 until valueArguments.size) {
            valueArguments[i] = irInvokeCall.valueArguments[invokeArgIndex++]
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun BirFunction.inline(target: BirDeclarationParent, arguments: List<BirValueDeclaration> = listOf()): BirReturnableBlock {
        val movedBody = body!!.move(this@inline, symbol, (explicitParameters zip arguments).toMap())
        return BirReturnableBlockImpl(this.sourceSpan, this.returnType, null, null).apply<BirReturnableBlockImpl> {
            this.statements += movedBody.statements
        }
    }

    private fun BirBody.move(
        source: BirFunction,
        targetSymbol: BirReturnTargetSymbol,
        arguments: Map<BirValueParameter, BirValueDeclaration>,
    ): BirBody {
        arguments.forEach { (oldParam, newParam) ->
            oldParam.getBackReferences(valueAccesses).forEach { paramUsage ->
                paramUsage.symbol = newParam.symbol
            }
        }

        source.getBackReferences(returnTargets).forEach { returnElement ->
            returnElement.returnTargetSymbol = targetSymbol
        }

        return this
    }
}