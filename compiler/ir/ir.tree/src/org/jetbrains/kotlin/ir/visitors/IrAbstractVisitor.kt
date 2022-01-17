/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

abstract class IrAbstractVisitor<out R, in D> {
    abstract fun visitElement(element: IrElement, data: D): R

    open fun visitModuleFragment(declaration: IrModuleFragment, data: D) = visitElement(declaration, data)
    open fun visitPackageFragment(declaration: IrPackageFragment, data: D) = visitElement(declaration, data)
    open fun visitFile(declaration: IrFile, data: D) = visitPackageFragment(declaration, data)
    open fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: D) = visitPackageFragment(declaration, data)
    open fun visitScript(declaration: IrScript, data: D) = visitDeclaration(declaration, data)

    open fun visitDeclaration(declaration: IrDeclarationBase, data: D) = visitElement(declaration, data)
    open fun visitClass(declaration: IrClass, data: D) = visitDeclaration(declaration, data)
    open fun visitFunction(declaration: IrFunction, data: D) = visitDeclaration(declaration, data)
    open fun visitSimpleFunction(declaration: IrSimpleFunction, data: D) = visitFunction(declaration, data)
    open fun visitConstructor(declaration: IrConstructor, data: D) = visitFunction(declaration, data)
    open fun visitProperty(declaration: IrProperty, data: D) = visitDeclaration(declaration, data)
    open fun visitField(declaration: IrField, data: D) = visitDeclaration(declaration, data)
    open fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: D) = visitDeclaration(declaration, data)
    open fun visitVariable(declaration: IrVariable, data: D) = visitDeclaration(declaration, data)
    open fun visitEnumEntry(declaration: IrEnumEntry, data: D) = visitDeclaration(declaration, data)
    open fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: D) = visitDeclaration(declaration, data)
    open fun visitTypeParameter(declaration: IrTypeParameter, data: D) = visitDeclaration(declaration, data)
    open fun visitValueParameter(declaration: IrValueParameter, data: D) = visitDeclaration(declaration, data)
    open fun visitTypeAlias(declaration: IrTypeAlias, data: D) = visitDeclaration(declaration, data)

    open fun visitBody(body: IrBody, data: D) = visitElement(body, data)
    open fun visitExpressionBody(body: IrExpressionBody, data: D) = visitBody(body, data)
    open fun visitBlockBody(body: IrBlockBody, data: D) = visitBody(body, data)
    open fun visitSyntheticBody(body: IrSyntheticBody, data: D) = visitBody(body, data)

    open fun visitSuspendableExpression(expression: IrSuspendableExpression, data: D) = visitExpression(expression, data)
    open fun visitSuspensionPoint(expression: IrSuspensionPoint, data: D) = visitExpression(expression, data)

    open fun visitExpression(expression: IrExpression, data: D) = visitElement(expression, data)
    open fun visitConst(expression: IrConst<*>, data: D) = visitExpression(expression, data)
    open fun visitConstantValue(expression: IrConstantValue, data: D) = visitExpression(expression, data)
    open fun visitConstantObject(expression: IrConstantObject, data: D) = visitConstantValue(expression, data)
    open fun visitConstantPrimitive(expression: IrConstantPrimitive, data: D) = visitConstantValue(expression, data)
    open fun visitConstantArray(expression: IrConstantArray, data: D) = visitConstantValue(expression, data)
    open fun visitVararg(expression: IrVararg, data: D) = visitExpression(expression, data)
    open fun visitSpreadElement(spread: IrSpreadElement, data: D) = visitElement(spread, data)

    open fun visitContainerExpression(expression: IrContainerExpression, data: D) = visitExpression(expression, data)
    open fun visitBlock(expression: IrBlock, data: D) = visitContainerExpression(expression, data)
    open fun visitComposite(expression: IrComposite, data: D) = visitContainerExpression(expression, data)
    open fun visitStringConcatenation(expression: IrStringConcatenation, data: D) = visitExpression(expression, data)

    open fun visitDeclarationReference(expression: IrDeclarationReference, data: D) = visitExpression(expression, data)
    open fun visitSingletonReference(expression: IrGetSingletonValue, data: D) = visitDeclarationReference(expression, data)
    open fun visitGetObjectValue(expression: IrGetObjectValue, data: D) = visitSingletonReference(expression, data)
    open fun visitGetEnumValue(expression: IrGetEnumValue, data: D) = visitSingletonReference(expression, data)
    open fun visitValueAccess(expression: IrValueAccessExpression, data: D) = visitDeclarationReference(expression, data)
    open fun visitGetValue(expression: IrGetValue, data: D) = visitValueAccess(expression, data)
    open fun visitSetValue(expression: IrSetValue, data: D) = visitValueAccess(expression, data)
    open fun visitFieldAccess(expression: IrFieldAccessExpression, data: D) = visitDeclarationReference(expression, data)
    open fun visitGetField(expression: IrGetField, data: D) = visitFieldAccess(expression, data)
    open fun visitSetField(expression: IrSetField, data: D) = visitFieldAccess(expression, data)

    open fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: D) = visitDeclarationReference(expression, data)
    open fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: D) = visitMemberAccess(expression, data)
    open fun visitCall(expression: IrCall, data: D) = visitFunctionAccess(expression, data)
    open fun visitConstructorCall(expression: IrConstructorCall, data: D) = visitFunctionAccess(expression, data)
    open fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: D) = visitFunctionAccess(expression, data)
    open fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: D) = visitFunctionAccess(expression, data)
    open fun visitGetClass(expression: IrGetClass, data: D) = visitExpression(expression, data)

    open fun visitCallableReference(expression: IrCallableReference<*>, data: D) = visitMemberAccess(expression, data)
    open fun visitFunctionReference(expression: IrFunctionReference, data: D) = visitCallableReference(expression, data)
    open fun visitPropertyReference(expression: IrPropertyReference, data: D) = visitCallableReference(expression, data)
    open fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: D) =
        visitCallableReference(expression, data)

    open fun visitRawFunctionReference(expression: IrRawFunctionReference, data: D) = visitDeclarationReference(expression, data)

    open fun visitFunctionExpression(expression: IrFunctionExpression, data: D) = visitExpression(expression, data)

    open fun visitClassReference(expression: IrClassReference, data: D) = visitDeclarationReference(expression, data)

    open fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: D) = visitExpression(expression, data)

    open fun visitTypeOperator(expression: IrTypeOperatorCall, data: D) = visitExpression(expression, data)

    open fun visitWhen(expression: IrWhen, data: D) = visitExpression(expression, data)
    open fun visitBranch(branch: IrBranch, data: D) = visitElement(branch, data)
    open fun visitElseBranch(branch: IrElseBranch, data: D) = visitBranch(branch, data)
    open fun visitLoop(loop: IrLoop, data: D) = visitExpression(loop, data)
    open fun visitWhileLoop(loop: IrWhileLoop, data: D) = visitLoop(loop, data)
    open fun visitDoWhileLoop(loop: IrDoWhileLoop, data: D) = visitLoop(loop, data)
    open fun visitTry(aTry: IrTry, data: D) = visitExpression(aTry, data)
    open fun visitCatch(aCatch: IrCatch, data: D) = visitElement(aCatch, data)

    open fun visitBreakContinue(jump: IrBreakContinue, data: D) = visitExpression(jump, data)
    open fun visitBreak(jump: IrBreak, data: D) = visitBreakContinue(jump, data)
    open fun visitContinue(jump: IrContinue, data: D) = visitBreakContinue(jump, data)

    open fun visitReturn(expression: IrReturn, data: D) = visitExpression(expression, data)
    open fun visitThrow(expression: IrThrow, data: D) = visitExpression(expression, data)

    open fun visitDynamicExpression(expression: IrDynamicExpression, data: D) = visitExpression(expression, data)
    open fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: D) = visitDynamicExpression(expression, data)
    open fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: D) = visitDynamicExpression(expression, data)

    open fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: D) = visitDeclaration(declaration, data)
    open fun visitErrorExpression(expression: IrErrorExpression, data: D) = visitExpression(expression, data)
    open fun visitErrorCallExpression(expression: IrErrorCallExpression, data: D) = visitErrorExpression(expression, data)
}
