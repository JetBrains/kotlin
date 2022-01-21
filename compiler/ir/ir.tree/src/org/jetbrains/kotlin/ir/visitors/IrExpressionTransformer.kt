/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

abstract class IrExpressionTransformer : IrElementVisitorVoid {

    protected abstract fun transformExpression(expression: IrExpression): IrExpression

    private fun IrExpression.transformPostfix(): IrExpression {
        acceptVoid(this@IrExpressionTransformer)
        return transformExpression(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        declaration.declarations.forEach { it.acceptVoid(this) }
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.valueParameters.forEach { it.acceptVoid(this) }
        declaration.body?.acceptVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.backingField?.acceptVoid(this)
        declaration.getter?.acceptVoid(this)
        declaration.setter?.acceptVoid(this)
    }

    override fun visitField(declaration: IrField) {
        declaration.initializer?.acceptVoid(this)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        declaration.delegate.acceptVoid(this)
        declaration.getter.acceptVoid(this)
        declaration.setter?.acceptVoid(this)
    }

    override fun visitVariable(declaration: IrVariable) {
        val initializer = declaration.initializer ?: return
        initializer.acceptVoid(this)
        declaration.initializer = transformExpression(initializer)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        val initializerBody = declaration.initializerExpression
        if (initializerBody != null) {
            initializerBody.expression = initializerBody.expression.transformPostfix()
        }
        declaration.correspondingClass?.acceptVoid(this)
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        declaration.body.acceptVoid(this)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {}

    override fun visitValueParameter(declaration: IrValueParameter) {
        val defaultValueBody = declaration.defaultValue ?: return
        defaultValueBody.expression = defaultValueBody.expression.transformPostfix()
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {}

    override fun visitExpressionBody(body: IrExpressionBody) {
        body.expression = body.expression.transformPostfix()
    }

    private fun MutableList<IrStatement>.transformStatements() {
        for (i in indices) {
            val statement = this[i]
            statement.acceptVoid(this@IrExpressionTransformer)
            val expression = statement as? IrExpression ?: continue
            this[i] = transformExpression(expression)
        }
    }

    private fun MutableList<IrExpression>.transformExpressions() {
        for (i in indices) {
            this[i] = this[i].transformPostfix()
        }
    }

    override fun visitBlockBody(body: IrBlockBody) {
        body.statements.transformStatements()
    }

    override fun visitSyntheticBody(body: IrSyntheticBody) {}

    override fun visitSuspendableExpression(expression: IrSuspendableExpression) {
        expression.suspensionPointId = expression.suspensionPointId.transformPostfix()
        expression.result = expression.result.transformPostfix()
    }

    override fun visitSuspensionPoint(expression: IrSuspensionPoint) {
        expression.suspensionPointIdParameter.acceptVoid(this)
        expression.result = expression.result.transformPostfix()
        expression.resumeResult = expression.resumeResult.transformPostfix()
    }

    override fun visitConst(expression: IrConst<*>) {}

    override fun visitConstantObject(expression: IrConstantObject) {
        // TODO do we ever need to transform individual constant values?
    }

    override fun visitConstantArray(expression: IrConstantArray) {
        // TODO do we ever need to transform individual constant values?
    }

    override fun visitConstantPrimitive(expression: IrConstantPrimitive) {
        // TODO do we ever need to transform individual constant values?
    }

    override fun visitVararg(expression: IrVararg) {
        for (i in expression.elements.indices) {
            when (val element = expression.elements[i]) {
                is IrExpression ->
                    expression.elements[i] = element.transformPostfix()
                is IrSpreadElement ->
                    element.acceptVoid(this)
            }
        }
    }

    override fun visitSpreadElement(spread: IrSpreadElement) {
        spread.expression = spread.expression.transformPostfix()
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        expression.statements.transformStatements()
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        expression.arguments.transformExpressions()
    }

    override fun visitSingletonReference(expression: IrGetSingletonValue) {}

    override fun visitGetValue(expression: IrGetValue) {}

    override fun visitSetValue(expression: IrSetValue) {
        expression.value = expression.value.transformPostfix()
    }

    override fun visitGetField(expression: IrGetField) {
        expression.receiver = expression.receiver?.transformPostfix()
    }

    override fun visitSetField(expression: IrSetField) {
        expression.receiver = expression.receiver?.transformPostfix()
        expression.value = expression.value.transformPostfix()
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
        expression.dispatchReceiver = expression.dispatchReceiver?.transformPostfix()
        expression.extensionReceiver = expression.extensionReceiver?.transformPostfix()
        for (i in 0 until expression.valueArgumentsCount) {
            expression.putValueArgument(i, expression.getValueArgument(i)?.transformPostfix())
        }
    }

    override fun visitRawFunctionReference(expression: IrRawFunctionReference) {}

    override fun visitFunctionExpression(expression: IrFunctionExpression) {
        expression.function.acceptVoid(this)
    }

    override fun visitClassReference(expression: IrClassReference) {}

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {}

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        expression.argument = expression.argument.transformPostfix()
    }

    override fun visitWhen(expression: IrWhen) {
        for (branch in expression.branches) {
            branch.condition = branch.condition.transformPostfix()
            branch.result = branch.result.transformPostfix()
        }
    }

    override fun visitLoop(loop: IrLoop) {
        loop.condition = loop.condition.transformPostfix()
        loop.body = loop.body?.transformPostfix()
    }

    override fun visitTry(aTry: IrTry) {
        aTry.tryResult = aTry.tryResult.transformPostfix()
        aTry.finallyExpression = aTry.finallyExpression?.transformPostfix()
        for (aCatch in aTry.catches) {
            aCatch.result = aCatch.result.transformPostfix()
        }
    }

    override fun visitBreakContinue(jump: IrBreakContinue) {}

    override fun visitReturn(expression: IrReturn) {
        expression.value = expression.value.transformPostfix()
    }

    override fun visitThrow(expression: IrThrow) {
        expression.value = expression.value.transformPostfix()
    }

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression) {
        expression.receiver = expression.receiver.transformPostfix()
        expression.arguments.transformExpressions()
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression) {
        expression.receiver = expression.receiver.transformPostfix()
    }
}