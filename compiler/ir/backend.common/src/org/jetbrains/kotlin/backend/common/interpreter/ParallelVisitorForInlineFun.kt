/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * This class is used for parallel processing two ir elements and gathering information about ir temporary variables from inline block
 */
internal class ParallelVisitorForInlineFun : IrElementVisitor<Unit, IrElement> {
    val originalVarToInline = mutableMapOf<DeclarationDescriptor, DeclarationDescriptor>()

    override fun visitElement(element: IrElement, data: IrElement) {
        element.accept(this, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: IrElement) {
        val original = expression.symbol.descriptor
        val inline = (data as IrGetValue).symbol.descriptor
        if (!original.equalTo(inline)) {
            originalVarToInline[original] = inline
        }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: IrElement) {}

    override fun visitVariable(declaration: IrVariable, data: IrElement) {
        declaration.initializer?.accept(this, (data as IrVariable).initializer!!)
    }

    override fun visitBody(body: IrBody, data: IrElement) {
        for ((index, statement) in body.statements.withIndex()) {
            statement.accept(this, (data as IrBody).statements[index])
        }
    }

    override fun visitBlock(expression: IrBlock, data: IrElement) {
        for ((index, statement) in expression.statements.withIndex()) {
            statement.accept(this, (data as IrBlock).statements[index])
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: IrElement) {
        for ((index, argument) in expression.arguments.withIndex()) {
            argument.accept(this, (data as IrStringConcatenation).arguments[index])
        }
    }

    override fun visitSetVariable(expression: IrSetVariable, data: IrElement) {
        expression.value.accept(this, (data as IrSetVariable).value)
    }

    override fun visitGetField(expression: IrGetField, data: IrElement) {
        expression.receiver?.accept(this, (data as IrGetField).receiver!!)
    }

    override fun visitSetField(expression: IrSetField, data: IrElement) {
        expression.value.accept(this, (data as IrSetField).value)
        expression.receiver?.accept(this, data.receiver!!)
    }

    override fun visitCall(expression: IrCall, data: IrElement) {
        expression.dispatchReceiver?.accept(this, (data as IrCall).dispatchReceiver!!)
        expression.extensionReceiver?.accept(this, (data as IrCall).extensionReceiver!!)
        for (i in 0 until expression.valueArgumentsCount) {
            expression.getValueArgument(i)?.accept(this, (data as IrCall).getValueArgument(i)!!)
        }
        expression.getBody()?.accept(this, (data as IrCall).getBody()!!)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: IrElement) {
        expression.dispatchReceiver?.accept(this, (data as IrConstructorCall).dispatchReceiver!!)
        expression.extensionReceiver?.accept(this, (data as IrConstructorCall).extensionReceiver!!)
        for (i in 0 until expression.valueArgumentsCount) {
            expression.getValueArgument(i)?.accept(this, (data as IrConstructorCall).getValueArgument(i)!!)
        }
        expression.getBody()?.accept(this, (data as IrConstructorCall).getBody()!!)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrElement) {
        expression.dispatchReceiver?.accept(this, (data as IrDelegatingConstructorCall).dispatchReceiver!!)
        expression.extensionReceiver?.accept(this, (data as IrDelegatingConstructorCall).extensionReceiver!!)
        for (i in 0 until expression.valueArgumentsCount) {
            expression.getValueArgument(i)?.accept(this, (data as IrDelegatingConstructorCall).getValueArgument(i)!!)
        }
        expression.getBody()?.accept(this, (data as IrDelegatingConstructorCall).getBody()!!)
    }

    override fun visitWhen(expression: IrWhen, data: IrElement) {
        for ((index, branch) in expression.branches.withIndex()) {
            branch.accept(this, (data as IrWhen).branches[index])
        }
    }

    override fun visitBranch(branch: IrBranch, data: IrElement) {
        branch.condition.accept(this, (data as IrBranch).condition)
        branch.result.accept(this, data.result)
    }

    override fun visitLoop(loop: IrLoop, data: IrElement) {
        loop.condition.accept(this, (data as IrLoop).condition)
        loop.body?.accept(this, data.body!!)
    }

    override fun visitTry(aTry: IrTry, data: IrElement) {
        aTry.tryResult.accept(this, (data as IrTry).tryResult)
        for ((index, catch) in aTry.catches.withIndex()) {
            catch.accept(this, data.catches[index])
        }
        aTry.finallyExpression?.accept(this, data.finallyExpression!!)
    }

    override fun visitCatch(aCatch: IrCatch, data: IrElement) {
        aCatch.catchParameter.accept(this, (data as IrCatch).catchParameter)
        aCatch.result.accept(this, data.result)
    }

    override fun visitReturn(expression: IrReturn, data: IrElement) {
        expression.value.accept(this, (data as IrReturn).value)
    }

    override fun visitThrow(expression: IrThrow, data: IrElement) {
        expression.value.accept(this, (data as IrThrow).value)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrElement) {
        expression.argument.accept(this, (data as IrTypeOperatorCall).argument)
    }
}