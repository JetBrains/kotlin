/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

abstract class IrElementTransformerVoidShallow : IrElementTransformerShallow<Nothing?>() {
    open fun visitElement(element: IrElement): IrElement {
        element.transformChildren(this, null)
        return element
    }

    final override fun visitElement(element: IrElement, data: Nothing?): IrElement = visitElement(element)

    open fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): IrModuleFragment =
        visitModuleFragment(declaration)

    open fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitPackageFragment(declaration: IrPackageFragment, data: Nothing?): IrElement = visitPackageFragment(declaration)

    open fun visitFile(declaration: IrFile): IrFile {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitFile(declaration: IrFile, data: Nothing?): IrFile = visitFile(declaration)

    open fun visitExternalPackageFragment(declaration: IrExternalPackageFragment): IrExternalPackageFragment {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): IrExternalPackageFragment =
        visitExternalPackageFragment(declaration)

    open fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitDeclaration(declaration: IrDeclarationBase, data: Nothing?): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    open fun visitScript(declaration: IrScript): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitScript(declaration: IrScript, data: Nothing?) = visitScript(declaration)

    open fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitClass(declaration: IrClass, data: Nothing?) = visitClass(declaration)

    open fun visitFunction(declaration: IrFunction): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitFunction(declaration: IrFunction, data: Nothing?): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    open fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?) = visitSimpleFunction(declaration)

    open fun visitConstructor(declaration: IrConstructor): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitConstructor(declaration: IrConstructor, data: Nothing?) = visitConstructor(declaration)

    open fun visitProperty(declaration: IrProperty): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitProperty(declaration: IrProperty, data: Nothing?) = visitProperty(declaration)

    open fun visitField(declaration: IrField): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitField(declaration: IrField, data: Nothing?) = visitField(declaration)

    open fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?) =
        visitLocalDelegatedProperty(declaration)

    open fun visitEnumEntry(declaration: IrEnumEntry): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?) = visitEnumEntry(declaration)

    open fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?) =
        visitAnonymousInitializer(declaration)

    open fun visitTypeParameter(declaration: IrTypeParameter): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): IrStatement = visitTypeParameter(declaration)

    open fun visitValueParameter(declaration: IrValueParameter): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): IrStatement = visitValueParameter(declaration)

    open fun visitVariable(declaration: IrVariable): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitVariable(declaration: IrVariable, data: Nothing?) = visitVariable(declaration)

    open fun visitTypeAlias(declaration: IrTypeAlias): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?) = visitTypeAlias(declaration)

    open fun visitBody(body: IrBody): IrBody {
        body.transformChildren(this, null)
        return body
    }

    final override fun visitBody(body: IrBody, data: Nothing?): IrBody = visitBody(body)

    open fun visitExpressionBody(body: IrExpressionBody): IrBody {
        body.transformChildren(this, null)
        return body
    }

    final override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?) = visitExpressionBody(body)

    open fun visitBlockBody(body: IrBlockBody): IrBody {
        body.transformChildren(this, null)
        return body
    }

    final override fun visitBlockBody(body: IrBlockBody, data: Nothing?) = visitBlockBody(body)

    open fun visitSyntheticBody(body: IrSyntheticBody): IrBody {
        body.transformChildren(this, null)
        return body
    }

    final override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?) = visitSyntheticBody(body)

    open fun visitSuspendableExpression(expression: IrSuspendableExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitSuspendableExpression(expression: IrSuspendableExpression, data: Nothing?) =
        visitSuspendableExpression(expression)

    open fun visitSuspensionPoint(expression: IrSuspensionPoint): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: Nothing?) = visitSuspensionPoint(expression)

    open fun visitExpression(expression: IrExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitExpression(expression: IrExpression, data: Nothing?): IrExpression = visitExpression(expression)

    open fun visitConst(expression: IrConst<*>): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitConst(expression: IrConst<*>, data: Nothing?) = visitConst(expression)

    open fun visitConstantValue(expression: IrConstantValue): IrConstantValue {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitConstantValue(expression: IrConstantValue, data: Nothing?) = visitConstantValue(expression)

    open fun visitConstantObject(expression: IrConstantObject): IrConstantValue {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitConstantObject(expression: IrConstantObject, data: Nothing?) = visitConstantObject(expression)

    open fun visitConstantPrimitive(expression: IrConstantPrimitive): IrConstantValue {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitConstantPrimitive(expression: IrConstantPrimitive, data: Nothing?) = visitConstantPrimitive(expression)

    open fun visitConstantArray(expression: IrConstantArray): IrConstantValue {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitConstantArray(expression: IrConstantArray, data: Nothing?) = visitConstantArray(expression)

    open fun visitVararg(expression: IrVararg): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitVararg(expression: IrVararg, data: Nothing?) = visitVararg(expression)

    open fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement {
        spread.transformChildren(this, null)
        return spread
    }

    final override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): IrSpreadElement = visitSpreadElement(spread)

    open fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitContainerExpression(expression: IrContainerExpression, data: Nothing?) = visitContainerExpression(expression)

    open fun visitBlock(expression: IrBlock): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitBlock(expression: IrBlock, data: Nothing?) = visitBlock(expression)

    open fun visitComposite(expression: IrComposite): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitComposite(expression: IrComposite, data: Nothing?) = visitComposite(expression)

    open fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?) = visitStringConcatenation(expression)

    open fun visitDeclarationReference(expression: IrDeclarationReference): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitDeclarationReference(expression: IrDeclarationReference, data: Nothing?) = visitDeclarationReference(expression)

    open fun visitSingletonReference(expression: IrGetSingletonValue): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitSingletonReference(expression: IrGetSingletonValue, data: Nothing?) = visitSingletonReference(expression)

    open fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?) = visitGetObjectValue(expression)

    open fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?) = visitGetEnumValue(expression)

    open fun visitValueAccess(expression: IrValueAccessExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitValueAccess(expression: IrValueAccessExpression, data: Nothing?) = visitValueAccess(expression)

    open fun visitGetValue(expression: IrGetValue): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitGetValue(expression: IrGetValue, data: Nothing?) = visitGetValue(expression)

    open fun visitSetValue(expression: IrSetValue): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitSetValue(expression: IrSetValue, data: Nothing?) = visitSetValue(expression)

    open fun visitFieldAccess(expression: IrFieldAccessExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitFieldAccess(expression: IrFieldAccessExpression, data: Nothing?): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    open fun visitGetField(expression: IrGetField): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitGetField(expression: IrGetField, data: Nothing?) = visitGetField(expression)

    open fun visitSetField(expression: IrSetField): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitSetField(expression: IrSetField, data: Nothing?) = visitSetField(expression)

    open fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: Nothing?) = visitMemberAccess(expression)

    open fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Nothing?) = visitFunctionAccess(expression)

    open fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitCall(expression: IrCall, data: Nothing?) = visitCall(expression)

    open fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?) = visitConstructorCall(expression)

    open fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?) =
        visitDelegatingConstructorCall(expression)

    open fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?) = visitEnumConstructorCall(expression)

    open fun visitGetClass(expression: IrGetClass): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitGetClass(expression: IrGetClass, data: Nothing?) = visitGetClass(expression)

    open fun visitCallableReference(expression: IrCallableReference<*>): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitCallableReference(expression: IrCallableReference<*>, data: Nothing?) = visitCallableReference(expression)

    open fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): IrElement =
        visitFunctionReference(expression)

    open fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): IrElement =
        visitPropertyReference(expression)

    open fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: Nothing?) =
        visitLocalDelegatedPropertyReference(expression)

    open fun visitRawFunctionReference(expression: IrRawFunctionReference): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: Nothing?) =
        visitRawFunctionReference(expression)

    open fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): IrElement =
        visitFunctionExpression(expression)

    open fun visitClassReference(expression: IrClassReference): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitClassReference(expression: IrClassReference, data: Nothing?) = visitClassReference(expression)

    open fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?) =
        visitInstanceInitializerCall(expression)

    open fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?) = visitTypeOperator(expression)

    open fun visitWhen(expression: IrWhen): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitWhen(expression: IrWhen, data: Nothing?) = visitWhen(expression)

    open fun visitBranch(branch: IrBranch): IrBranch {
        branch.transformChildren(this, null)
        return branch
    }

    final override fun visitBranch(branch: IrBranch, data: Nothing?): IrBranch = visitBranch(branch)

    open fun visitElseBranch(branch: IrElseBranch): IrElseBranch {
        branch.transformChildren(this, null)
        return branch
    }

    final override fun visitElseBranch(branch: IrElseBranch, data: Nothing?): IrElseBranch = visitElseBranch(branch)

    open fun visitLoop(loop: IrLoop): IrExpression {
        throw UnsupportedOperationException("Must be overridden")
    }

    final override fun visitLoop(loop: IrLoop, data: Nothing?) = visitLoop(loop)

    open fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        loop.transformChildren(this, null)
        return loop
    }

    final override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?) = visitWhileLoop(loop)

    open fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression {
        loop.transformChildren(this, null)
        return loop
    }

    final override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?) = visitDoWhileLoop(loop)

    open fun visitTry(aTry: IrTry) = visitExpression(aTry)
    final override fun visitTry(aTry: IrTry, data: Nothing?) = visitTry(aTry)

    open fun visitCatch(aCatch: IrCatch): IrCatch {
        aCatch.transformChildren(this, null)
        return aCatch
    }

    final override fun visitCatch(aCatch: IrCatch, data: Nothing?): IrCatch = visitCatch(aCatch)

    open fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
        throw UnsupportedOperationException("Must be overridden")
    }

    final override fun visitBreakContinue(jump: IrBreakContinue, data: Nothing?) = visitBreakContinue(jump)

    open fun visitBreak(jump: IrBreak): IrExpression {
        jump.transformChildren(this, null)
        return jump
    }

    final override fun visitBreak(jump: IrBreak, data: Nothing?) = visitBreak(jump)

    open fun visitContinue(jump: IrContinue): IrExpression {
        jump.transformChildren(this, null)
        return jump
    }

    final override fun visitContinue(jump: IrContinue, data: Nothing?) = visitContinue(jump)

    open fun visitReturn(expression: IrReturn): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitReturn(expression: IrReturn, data: Nothing?) = visitReturn(expression)

    open fun visitThrow(expression: IrThrow): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitThrow(expression: IrThrow, data: Nothing?) = visitThrow(expression)

    open fun visitDynamicExpression(expression: IrDynamicExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitDynamicExpression(expression: IrDynamicExpression, data: Nothing?) = visitDynamicExpression(expression)

    open fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: Nothing?) =
        visitDynamicOperatorExpression(expression)

    open fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: Nothing?) =
        visitDynamicMemberExpression(expression)

    open fun visitErrorDeclaration(declaration: IrErrorDeclaration): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    final override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?) = visitErrorDeclaration(declaration)

    open fun visitErrorExpression(expression: IrErrorExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?) = visitErrorExpression(expression)

    open fun visitErrorCallExpression(expression: IrErrorCallExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    final override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?) = visitErrorCallExpression(expression)

    protected inline fun <T : IrElement> T.transformPostfix(body: T.() -> Unit): T {
        transformChildrenVoid()
        this.body()
        return this
    }

    protected fun IrElement.transformChildrenVoid() {
        transformChildrenVoid(this@IrElementTransformerVoidShallow)
    }
}

fun IrElement.transformChildrenVoid(transformer: IrElementTransformerVoidShallow) {
    transformChildren(transformer, null)
}