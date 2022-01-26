/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

interface IrThinVisitor<out R, in D> {
    fun visitElement(element: IrElement, data: D): R

    fun visitModuleFragment(declaration: IrModuleFragment, data: D): R =
        visitElement(declaration, data)

    fun visitFile(declaration: IrFile, data: D): R =
        visitElement(declaration, data)

    fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: D): R =
        visitElement(declaration, data)

    fun visitScript(declaration: IrScript, data: D): R =
        visitElement(declaration, data)

    fun visitClass(declaration: IrClass, data: D): R =
        visitElement(declaration, data)

    fun visitSimpleFunction(declaration: IrSimpleFunction, data: D): R =
        visitElement(declaration, data)

    fun visitConstructor(declaration: IrConstructor, data: D): R =
        visitElement(declaration, data)

    fun visitProperty(declaration: IrProperty, data: D) =
        visitElement(declaration, data)

    fun visitField(declaration: IrField, data: D) =
        visitElement(declaration, data)

    fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: D) =
        visitElement(declaration, data)

    fun visitVariable(declaration: IrVariable, data: D) =
        visitElement(declaration, data)

    fun visitEnumEntry(declaration: IrEnumEntry, data: D) =
        visitElement(declaration, data)

    fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: D) =
        visitElement(declaration, data)

    fun visitTypeParameter(declaration: IrTypeParameter, data: D) =
        visitElement(declaration, data)

    fun visitValueParameter(declaration: IrValueParameter, data: D) =
        visitElement(declaration, data)

    fun visitTypeAlias(declaration: IrTypeAlias, data: D) =
        visitElement(declaration, data)

    fun visitExpressionBody(body: IrExpressionBody, data: D) =
        visitElement(body, data)

    fun visitBlockBody(body: IrBlockBody, data: D) =
        visitElement(body, data)

    fun visitSyntheticBody(body: IrSyntheticBody, data: D) =
        visitElement(body, data)

    fun visitSuspendableExpression(expression: IrSuspendableExpression, data: D) =
        visitElement(expression, data)

    fun visitSuspensionPoint(expression: IrSuspensionPoint, data: D) =
        visitElement(expression, data)

    fun visitConst(expression: IrConst<*>, data: D) =
        visitElement(expression, data)

    fun visitConstantObject(expression: IrConstantObject, data: D) =
        visitElement(expression, data)

    fun visitConstantPrimitive(expression: IrConstantPrimitive, data: D) =
        visitElement(expression, data)

    fun visitConstantArray(expression: IrConstantArray, data: D) =
        visitElement(expression, data)

    fun visitVararg(expression: IrVararg, data: D) =
        visitElement(expression, data)

    fun visitSpreadElement(spread: IrSpreadElement, data: D) =
        visitElement(spread, data)

    fun visitBlock(expression: IrBlock, data: D) =
        visitElement(expression, data)

    fun visitComposite(expression: IrComposite, data: D) =
        visitElement(expression, data)

    fun visitStringConcatenation(expression: IrStringConcatenation, data: D) =
        visitElement(expression, data)

    fun visitGetObjectValue(expression: IrGetObjectValue, data: D) =
        visitElement(expression, data)

    fun visitGetEnumValue(expression: IrGetEnumValue, data: D) =
        visitElement(expression, data)

    fun visitGetValue(expression: IrGetValue, data: D) =
        visitElement(expression, data)

    fun visitSetValue(expression: IrSetValue, data: D) =
        visitElement(expression, data)

    fun visitGetField(expression: IrGetField, data: D) =
        visitElement(expression, data)

    fun visitSetField(expression: IrSetField, data: D) =
        visitElement(expression, data)

    fun visitCall(expression: IrCall, data: D) =
        visitElement(expression, data)

    fun visitConstructorCall(expression: IrConstructorCall, data: D) =
        visitElement(expression, data)

    fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: D) =
        visitElement(expression, data)

    fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: D) =
        visitElement(expression, data)

    fun visitGetClass(expression: IrGetClass, data: D) =
        visitElement(expression, data)

    fun visitFunctionReference(expression: IrFunctionReference, data: D) =
        visitElement(expression, data)

    fun visitPropertyReference(expression: IrPropertyReference, data: D) =
        visitElement(expression, data)

    fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: D) =
        visitElement(expression, data)

    fun visitRawFunctionReference(expression: IrRawFunctionReference, data: D) =
        visitElement(expression, data)

    fun visitFunctionExpression(expression: IrFunctionExpression, data: D) =
        visitElement(expression, data)

    fun visitClassReference(expression: IrClassReference, data: D) =
        visitElement(expression, data)

    fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: D) =
        visitElement(expression, data)

    fun visitTypeOperator(expression: IrTypeOperatorCall, data: D) =
        visitElement(expression, data)

    fun visitWhen(expression: IrWhen, data: D) =
        visitElement(expression, data)

    fun visitBranch(branch: IrBranch, data: D) =
        visitElement(branch, data)

    fun visitElseBranch(branch: IrElseBranch, data: D) =
        visitBranch(branch, data)

    fun visitWhileLoop(loop: IrWhileLoop, data: D) =
        visitElement(loop, data)

    fun visitDoWhileLoop(loop: IrDoWhileLoop, data: D) =
        visitElement(loop, data)

    fun visitTry(aTry: IrTry, data: D) =
        visitElement(aTry, data)

    fun visitCatch(aCatch: IrCatch, data: D) =
        visitElement(aCatch, data)

    fun visitBreak(jump: IrBreak, data: D) =
        visitElement(jump, data)

    fun visitContinue(jump: IrContinue, data: D) =
        visitElement(jump, data)

    fun visitReturn(expression: IrReturn, data: D) =
        visitElement(expression, data)

    fun visitThrow(expression: IrThrow, data: D) =
        visitElement(expression, data)

    fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: D) =
        visitElement(expression, data)

    fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: D) =
        visitElement(expression, data)

    fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: D) =
        visitElement(declaration, data)

    fun visitErrorExpression(expression: IrErrorExpression, data: D) =
        visitElement(expression, data)

    fun visitErrorCallExpression(expression: IrErrorCallExpression, data: D) =
        visitErrorExpression(expression, data)
}
