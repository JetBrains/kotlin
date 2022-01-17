/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

abstract class IrAbstractVisitorVoid : IrAbstractVisitor<Unit, Nothing?>() {
    abstract fun visitElement(element: IrElement)
    override fun visitElement(element: IrElement, data: Nothing?) = visitElement(element)

    open fun visitModuleFragment(declaration: IrModuleFragment) = visitElement(declaration)
    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?) = visitModuleFragment(declaration)

    open fun visitPackageFragment(declaration: IrPackageFragment) = visitElement(declaration)
    override fun visitPackageFragment(declaration: IrPackageFragment, data: Nothing?) = visitPackageFragment(declaration)

    open fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) = visitPackageFragment(declaration)
    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?) =
        visitExternalPackageFragment(declaration)

    open fun visitFile(declaration: IrFile) = visitPackageFragment(declaration)
    override fun visitFile(declaration: IrFile, data: Nothing?) = visitFile(declaration)

    open fun visitDeclaration(declaration: IrDeclarationBase) = visitElement(declaration)
    override fun visitDeclaration(declaration: IrDeclarationBase, data: Nothing?) = visitDeclaration(declaration)

    open fun visitClass(declaration: IrClass) = visitDeclaration(declaration)
    override fun visitClass(declaration: IrClass, data: Nothing?) = visitClass(declaration)

    open fun visitScript(declaration: IrScript) = visitDeclaration(declaration)
    override fun visitScript(declaration: IrScript, data: Nothing?) = visitScript(declaration)

    open fun visitFunction(declaration: IrFunction) = visitDeclaration(declaration)
    override fun visitFunction(declaration: IrFunction, data: Nothing?) = visitFunction(declaration)

    open fun visitSimpleFunction(declaration: IrSimpleFunction) = visitFunction(declaration)
    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?) = visitSimpleFunction(declaration)

    open fun visitConstructor(declaration: IrConstructor) = visitFunction(declaration)
    override fun visitConstructor(declaration: IrConstructor, data: Nothing?) = visitConstructor(declaration)

    open fun visitProperty(declaration: IrProperty) = visitDeclaration(declaration)
    override fun visitProperty(declaration: IrProperty, data: Nothing?) = visitProperty(declaration)

    open fun visitField(declaration: IrField) = visitDeclaration(declaration)
    override fun visitField(declaration: IrField, data: Nothing?) = visitField(declaration)

    open fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) = visitDeclaration(declaration)
    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?) =
        visitLocalDelegatedProperty(declaration)

    open fun visitVariable(declaration: IrVariable) = visitDeclaration(declaration)
    override fun visitVariable(declaration: IrVariable, data: Nothing?) = visitVariable(declaration)

    open fun visitEnumEntry(declaration: IrEnumEntry) = visitDeclaration(declaration)
    override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?) = visitEnumEntry(declaration)

    open fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) = visitDeclaration(declaration)
    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?) = visitAnonymousInitializer(declaration)

    open fun visitTypeParameter(declaration: IrTypeParameter) = visitDeclaration(declaration)
    override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?) = visitTypeParameter(declaration)

    open fun visitValueParameter(declaration: IrValueParameter) = visitDeclaration(declaration)
    override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?) = visitValueParameter(declaration)

    open fun visitTypeAlias(declaration: IrTypeAlias) = visitDeclaration(declaration)
    override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?) = visitTypeAlias(declaration)

    open fun visitBody(body: IrBody) = visitElement(body)
    override fun visitBody(body: IrBody, data: Nothing?) = visitBody(body)

    open fun visitExpressionBody(body: IrExpressionBody) = visitBody(body)
    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?) = visitExpressionBody(body)

    open fun visitBlockBody(body: IrBlockBody) = visitBody(body)
    override fun visitBlockBody(body: IrBlockBody, data: Nothing?) = visitBlockBody(body)

    open fun visitSyntheticBody(body: IrSyntheticBody) = visitBody(body)
    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?) = visitSyntheticBody(body)

    open fun visitSuspendableExpression(expression: IrSuspendableExpression) = visitExpression(expression)
    override fun visitSuspendableExpression(expression: IrSuspendableExpression, data: Nothing?) = visitSuspendableExpression(expression)

    open fun visitSuspensionPoint(expression: IrSuspensionPoint) = visitExpression(expression)
    override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: Nothing?) = visitSuspensionPoint(expression)

    open fun visitExpression(expression: IrExpression) = visitElement(expression)
    override fun visitExpression(expression: IrExpression, data: Nothing?) = visitExpression(expression)

    open fun visitConst(expression: IrConst<*>) = visitExpression(expression)
    override fun visitConst(expression: IrConst<*>, data: Nothing?) = visitConst(expression)

    open fun visitConstantValue(expression: IrConstantValue) = visitExpression(expression)
    override fun visitConstantValue(expression: IrConstantValue, data: Nothing?) = visitConstantValue(expression)

    open fun visitConstantObject(expression: IrConstantObject) = visitConstantValue(expression)
    override fun visitConstantObject(expression: IrConstantObject, data: Nothing?) = visitConstantObject(expression)

    open fun visitConstantPrimitive(expression: IrConstantPrimitive) = visitConstantValue(expression)
    override fun visitConstantPrimitive(expression: IrConstantPrimitive, data: Nothing?) = visitConstantPrimitive(expression)

    open fun visitConstantArray(expression: IrConstantArray) = visitConstantValue(expression)
    override fun visitConstantArray(expression: IrConstantArray, data: Nothing?) = visitConstantArray(expression)

    open fun visitVararg(expression: IrVararg) = visitExpression(expression)
    override fun visitVararg(expression: IrVararg, data: Nothing?) = visitVararg(expression)

    open fun visitSpreadElement(spread: IrSpreadElement) = visitElement(spread)
    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?) = visitSpreadElement(spread)

    open fun visitContainerExpression(expression: IrContainerExpression) = visitExpression(expression)
    override fun visitContainerExpression(expression: IrContainerExpression, data: Nothing?) = visitContainerExpression(expression)

    open fun visitComposite(expression: IrComposite) = visitContainerExpression(expression)
    override fun visitComposite(expression: IrComposite, data: Nothing?) = visitComposite(expression)

    open fun visitBlock(expression: IrBlock) = visitContainerExpression(expression)
    override fun visitBlock(expression: IrBlock, data: Nothing?) = visitBlock(expression)

    open fun visitStringConcatenation(expression: IrStringConcatenation) = visitExpression(expression)
    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?) = visitStringConcatenation(expression)

    open fun visitDeclarationReference(expression: IrDeclarationReference) = visitExpression(expression)
    override fun visitDeclarationReference(expression: IrDeclarationReference, data: Nothing?) = visitDeclarationReference(expression)

    open fun visitSingletonReference(expression: IrGetSingletonValue) = visitDeclarationReference(expression)
    override fun visitSingletonReference(expression: IrGetSingletonValue, data: Nothing?) = visitSingletonReference(expression)

    open fun visitGetObjectValue(expression: IrGetObjectValue) = visitSingletonReference(expression)
    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?) = visitGetObjectValue(expression)

    open fun visitGetEnumValue(expression: IrGetEnumValue) = visitSingletonReference(expression)
    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?) = visitGetEnumValue(expression)

    open fun visitVariableAccess(expression: IrValueAccessExpression) = visitDeclarationReference(expression)
    override fun visitValueAccess(expression: IrValueAccessExpression, data: Nothing?) = visitVariableAccess(expression)

    open fun visitGetValue(expression: IrGetValue) = visitVariableAccess(expression)
    override fun visitGetValue(expression: IrGetValue, data: Nothing?) = visitGetValue(expression)

    open fun visitSetValue(expression: IrSetValue) = visitVariableAccess(expression)
    override fun visitSetValue(expression: IrSetValue, data: Nothing?) = visitSetValue(expression)

    open fun visitFieldAccess(expression: IrFieldAccessExpression) = visitDeclarationReference(expression)
    override fun visitFieldAccess(expression: IrFieldAccessExpression, data: Nothing?) = visitFieldAccess(expression)

    open fun visitGetField(expression: IrGetField) = visitFieldAccess(expression)
    override fun visitGetField(expression: IrGetField, data: Nothing?) = visitGetField(expression)

    open fun visitSetField(expression: IrSetField) = visitFieldAccess(expression)
    override fun visitSetField(expression: IrSetField, data: Nothing?) = visitSetField(expression)

    open fun visitMemberAccess(expression: IrMemberAccessExpression<*>) = visitDeclarationReference(expression)
    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: Nothing?) = visitMemberAccess(expression)

    open fun visitFunctionAccess(expression: IrFunctionAccessExpression) = visitMemberAccess(expression)
    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Nothing?) = visitFunctionAccess(expression)

    open fun visitCall(expression: IrCall) = visitFunctionAccess(expression)
    override fun visitCall(expression: IrCall, data: Nothing?) = visitCall(expression)

    open fun visitConstructorCall(expression: IrConstructorCall) = visitFunctionAccess(expression)
    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?) = visitConstructorCall(expression)

    open fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) = visitFunctionAccess(expression)
    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?) =
        visitDelegatingConstructorCall(expression)

    open fun visitEnumConstructorCall(expression: IrEnumConstructorCall) = visitFunctionAccess(expression)
    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?) = visitEnumConstructorCall(expression)

    open fun visitGetClass(expression: IrGetClass) = visitExpression(expression)
    override fun visitGetClass(expression: IrGetClass, data: Nothing?) = visitGetClass(expression)

    open fun visitCallableReference(expression: IrCallableReference<*>) = visitMemberAccess(expression)
    override fun visitCallableReference(expression: IrCallableReference<*>, data: Nothing?) = visitCallableReference(expression)

    open fun visitFunctionReference(expression: IrFunctionReference) = visitCallableReference(expression)
    override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?) = visitFunctionReference(expression)

    open fun visitPropertyReference(expression: IrPropertyReference) = visitCallableReference(expression)
    override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?) = visitPropertyReference(expression)

    open fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) = visitCallableReference(expression)
    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: Nothing?) =
        visitLocalDelegatedPropertyReference(expression)

    open fun visitRawFunctionReference(expression: IrRawFunctionReference) = visitDeclarationReference(expression)
    override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: Nothing?) =
        visitRawFunctionReference(expression)

    open fun visitFunctionExpression(expression: IrFunctionExpression) = visitExpression(expression)
    override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?) = visitFunctionExpression(expression)

    open fun visitClassReference(expression: IrClassReference) = visitDeclarationReference(expression)
    override fun visitClassReference(expression: IrClassReference, data: Nothing?) = visitClassReference(expression)

    open fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) = visitExpression(expression)
    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?) =
        visitInstanceInitializerCall(expression)

    open fun visitTypeOperator(expression: IrTypeOperatorCall) = visitExpression(expression)
    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?) = visitTypeOperator(expression)

    open fun visitWhen(expression: IrWhen) = visitExpression(expression)
    override fun visitWhen(expression: IrWhen, data: Nothing?) = visitWhen(expression)

    open fun visitBranch(branch: IrBranch) = visitElement(branch)
    override fun visitBranch(branch: IrBranch, data: Nothing?) = visitBranch(branch)

    open fun visitElseBranch(branch: IrElseBranch) = visitBranch(branch)
    override fun visitElseBranch(branch: IrElseBranch, data: Nothing?) = visitElseBranch(branch)

    open fun visitLoop(loop: IrLoop) = visitExpression(loop)
    override fun visitLoop(loop: IrLoop, data: Nothing?) = visitLoop(loop)

    open fun visitWhileLoop(loop: IrWhileLoop) = visitLoop(loop)
    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?) = visitWhileLoop(loop)

    open fun visitDoWhileLoop(loop: IrDoWhileLoop) = visitLoop(loop)
    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?) = visitDoWhileLoop(loop)

    open fun visitTry(aTry: IrTry) = visitExpression(aTry)
    override fun visitTry(aTry: IrTry, data: Nothing?) = visitTry(aTry)

    open fun visitCatch(aCatch: IrCatch) = visitElement(aCatch)
    override fun visitCatch(aCatch: IrCatch, data: Nothing?) = visitCatch(aCatch)

    open fun visitBreakContinue(jump: IrBreakContinue) = visitExpression(jump)
    override fun visitBreakContinue(jump: IrBreakContinue, data: Nothing?) = visitBreakContinue(jump)

    open fun visitBreak(jump: IrBreak) = visitBreakContinue(jump)
    override fun visitBreak(jump: IrBreak, data: Nothing?) = visitBreak(jump)

    open fun visitContinue(jump: IrContinue) = visitBreakContinue(jump)
    override fun visitContinue(jump: IrContinue, data: Nothing?) = visitContinue(jump)

    open fun visitReturn(expression: IrReturn) = visitExpression(expression)
    override fun visitReturn(expression: IrReturn, data: Nothing?) = visitReturn(expression)

    open fun visitThrow(expression: IrThrow) = visitExpression(expression)
    override fun visitThrow(expression: IrThrow, data: Nothing?) = visitThrow(expression)

    open fun visitDynamicExpression(expression: IrDynamicExpression) = visitExpression(expression)
    override fun visitDynamicExpression(expression: IrDynamicExpression, data: Nothing?) = visitDynamicExpression(expression)

    open fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression) = visitDynamicExpression(expression)
    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: Nothing?) =
        visitDynamicOperatorExpression(expression)

    open fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression) = visitDynamicExpression(expression)
    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: Nothing?) =
        visitDynamicMemberExpression(expression)

    open fun visitErrorDeclaration(declaration: IrErrorDeclaration) = visitDeclaration(declaration)
    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?) = visitErrorDeclaration(declaration)

    open fun visitErrorExpression(expression: IrErrorExpression) = visitExpression(expression)
    override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?) = visitErrorExpression(expression)

    open fun visitErrorCallExpression(expression: IrErrorCallExpression) = visitErrorExpression(expression)
    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?) = visitErrorCallExpression(expression)
}