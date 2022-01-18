/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

abstract class IrThinVisitorVoid : IrThinVisitor<Unit, Nothing?>() {

    abstract fun visitElement(element: IrElement)
    
    final override fun visitElement(element: IrElement, data: Nothing?) {
        visitElement(element)
    }
    
    open fun visitModuleFragment(declaration: IrModuleFragment) {
        visitElement(declaration)
    }
    
    final override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?) {
        visitModuleFragment(declaration)
    }

    open fun visitFile(declaration: IrFile) {
        visitElement(declaration)
    } 
    
    final override fun visitFile(declaration: IrFile, data: Nothing?) {
        visitFile(declaration)
    }

    open fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) {
        visitElement(declaration)
    }

    final override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?) {
        visitExternalPackageFragment(declaration)
    }

    open fun visitScript(declaration: IrScript) {
        visitElement(declaration)
    }

    final override fun visitScript(declaration: IrScript, data: Nothing?) {
        visitScript(declaration)
    }

    open fun visitClass(declaration: IrClass) {
        visitElement(declaration)
    }

    final override fun visitClass(declaration: IrClass, data: Nothing?) {
        visitClass(declaration)
    }

    open fun visitSimpleFunction(declaration: IrSimpleFunction) {
        visitElement(declaration)
    }

    final override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?) {
        visitSimpleFunction(declaration)
    }

    open fun visitConstructor(declaration: IrConstructor) {
        visitElement(declaration)
    }

    final override fun visitConstructor(declaration: IrConstructor, data: Nothing?) {
        visitConstructor(declaration)
    }

    open fun visitProperty(declaration: IrProperty) {
        visitElement(declaration)
    }

    final override fun visitProperty(declaration: IrProperty, data: Nothing?) {
        visitProperty(declaration)
    }

    open fun visitField(declaration: IrField) {
        visitElement(declaration)
    }

    final override fun visitField(declaration: IrField, data: Nothing?) {
        visitField(declaration)
    }

    open fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        visitElement(declaration)
    }

    final override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?) {
        visitLocalDelegatedProperty(declaration)
    }

    open fun visitVariable(declaration: IrVariable) {
        visitElement(declaration)
    }

    final override fun visitVariable(declaration: IrVariable, data: Nothing?) {
        visitVariable(declaration)
    }

    open fun visitEnumEntry(declaration: IrEnumEntry) {
        visitElement(declaration)
    }

    final override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?) {
        visitEnumEntry(declaration)
    }

    open fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        visitElement(declaration)
    }

    final override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?) {
        visitAnonymousInitializer(declaration)
    }

    open fun visitTypeParameter(declaration: IrTypeParameter) {
        visitElement(declaration)
    }

    final override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?) {
        visitTypeParameter(declaration)
    }

    open fun visitValueParameter(declaration: IrValueParameter) {
        visitElement(declaration)
    }

    final override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?) {
        visitValueParameter(declaration)
    }

    open fun visitTypeAlias(declaration: IrTypeAlias) {
        visitElement(declaration)
    }

    final override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?) {
        visitTypeAlias(declaration)
    }

    open fun visitExpressionBody(body: IrExpressionBody) {
        visitElement(body)
    }

    final override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?) {
        visitExpressionBody(body)
    }

    open fun visitBlockBody(body: IrBlockBody) {
        visitElement(body)
    }

    final override fun visitBlockBody(body: IrBlockBody, data: Nothing?) {
        visitBlockBody(body)
    }

    open fun visitSyntheticBody(body: IrSyntheticBody) {
        visitElement(body)
    }

    final override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?) {
        visitSyntheticBody(body)
    }

    open fun visitSuspendableExpression(expression: IrSuspendableExpression) {
        visitElement(expression)
    }

    final override fun visitSuspendableExpression(expression: IrSuspendableExpression, data: Nothing?) {
        visitSuspendableExpression(expression)
    }

    open fun visitSuspensionPoint(expression: IrSuspensionPoint) {
        visitElement(expression)
    }

    final override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: Nothing?) {
        visitSuspensionPoint(expression)
    }

    open fun visitConst(expression: IrConst<*>) {
        visitElement(expression)
    }

    final override fun visitConst(expression: IrConst<*>, data: Nothing?) {
        visitConst(expression)
    }

    open fun visitConstantObject(expression: IrConstantObject) {
        visitElement(expression)
    }

    final override fun visitConstantObject(expression: IrConstantObject, data: Nothing?) {
        visitConstantObject(expression)
    }

    open fun visitConstantPrimitive(expression: IrConstantPrimitive) {
        visitElement(expression)
    }

    final override fun visitConstantPrimitive(expression: IrConstantPrimitive, data: Nothing?) {
        visitConstantPrimitive(expression)
    }

    open fun visitConstantArray(expression: IrConstantArray) {
        visitElement(expression)
    }

    final override fun visitConstantArray(expression: IrConstantArray, data: Nothing?) {
        visitConstantArray(expression)
    }

    open fun visitVararg(expression: IrVararg) {
        visitElement(expression)
    }

    final override fun visitVararg(expression: IrVararg, data: Nothing?) {
        visitVararg(expression)
    }

    open fun visitSpreadElement(spread: IrSpreadElement) {
        visitElement(spread)
    }

    final override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?) {
        visitSpreadElement(spread)
    }

    open fun visitBlock(expression: IrBlock) {
        visitElement(expression)
    }

    final override fun visitBlock(expression: IrBlock, data: Nothing?) {
        visitBlock(expression)
    }

    open fun visitComposite(expression: IrComposite) {
        visitElement(expression)
    }

    final override fun visitComposite(expression: IrComposite, data: Nothing?) {
        visitComposite(expression)
    }

    open fun visitStringConcatenation(expression: IrStringConcatenation) {
        visitElement(expression)
    }

    final override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?) {
        visitStringConcatenation(expression)
    }

    open fun visitGetObjectValue(expression: IrGetObjectValue) {
        visitElement(expression)
    }

    final override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?) {
        visitGetObjectValue(expression)
    }

    open fun visitGetEnumValue(expression: IrGetEnumValue) {
        visitElement(expression)
    }

    final override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?) {
        visitGetEnumValue(expression)
    }

    open fun visitGetValue(expression: IrGetValue) {
        visitElement(expression)
    }

    final override fun visitGetValue(expression: IrGetValue, data: Nothing?) {
        visitGetValue(expression)
    }

    open fun visitSetValue(expression: IrSetValue) {
        visitElement(expression)
    }

    final override fun visitSetValue(expression: IrSetValue, data: Nothing?) {
        visitSetValue(expression)
    }

    open fun visitGetField(expression: IrGetField) {
        visitElement(expression)
    }

    final override fun visitGetField(expression: IrGetField, data: Nothing?) {
        visitGetField(expression)
    }

    open fun visitSetField(expression: IrSetField) {
        visitElement(expression)
    }

    final override fun visitSetField(expression: IrSetField, data: Nothing?) {
        visitSetField(expression)
    }

    open fun visitCall(expression: IrCall) {
        visitElement(expression)
    }

    final override fun visitCall(expression: IrCall, data: Nothing?) {
        visitCall(expression)
    }

    open fun visitConstructorCall(expression: IrConstructorCall) {
        visitElement(expression)
    }

    final override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?) {
        visitConstructorCall(expression)
    }

    open fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        visitElement(expression)
    }

    final override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?) {
        visitDelegatingConstructorCall(expression)
    }

    open fun visitEnumConstructorCall(expression: IrEnumConstructorCall) {
        visitElement(expression)
    }

    final override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?) {
        visitEnumConstructorCall(expression)
    }

    open fun visitGetClass(expression: IrGetClass) {
        visitElement(expression)
    }

    final override fun visitGetClass(expression: IrGetClass, data: Nothing?) {
        visitGetClass(expression)
    }

    open fun visitFunctionReference(expression: IrFunctionReference) {
        visitElement(expression)
    }

    final override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?) {
        visitFunctionReference(expression)
    }

    open fun visitPropertyReference(expression: IrPropertyReference) {
        visitElement(expression)
    }

    final override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?) {
        visitPropertyReference(expression)
    }

    open fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        visitElement(expression)
    }

    final override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: Nothing?) {
        visitLocalDelegatedPropertyReference(expression)
    }

    open fun visitRawFunctionReference(expression: IrRawFunctionReference) {
        visitElement(expression)
    }

    final override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: Nothing?) {
        visitRawFunctionReference(expression)
    }

    open fun visitFunctionExpression(expression: IrFunctionExpression) {
        visitElement(expression)
    }

    final override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?) {
        visitFunctionExpression(expression)
    }

    open fun visitClassReference(expression: IrClassReference) {
        visitElement(expression)
    }

    final override fun visitClassReference(expression: IrClassReference, data: Nothing?) {
        visitClassReference(expression)
    }

    open fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        visitElement(expression)
    }

    final override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?) {
        visitInstanceInitializerCall(expression)
    }

    open fun visitTypeOperator(expression: IrTypeOperatorCall) {
        visitElement(expression)
    }

    final override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?) {
        visitTypeOperator(expression)
    }

    open fun visitWhen(expression: IrWhen) {
        visitElement(expression)
    }

    final override fun visitWhen(expression: IrWhen, data: Nothing?) {
        visitWhen(expression)
    }

    open fun visitBranch(branch: IrBranch) {
        visitElement(branch)
    }

    final override fun visitBranch(branch: IrBranch, data: Nothing?) {
        visitBranch(branch)
    }

    open fun visitElseBranch(branch: IrElseBranch) {
        visitBranch(branch)
    }

    final override fun visitElseBranch(branch: IrElseBranch, data: Nothing?) {
        visitElseBranch(branch)
    }

    open fun visitWhileLoop(loop: IrWhileLoop) {
        visitElement(loop)
    }

    final override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?) {
        visitWhileLoop(loop)
    }

    open fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        visitElement(loop)
    }

    final override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?) {
        visitDoWhileLoop(loop)
    }

    open fun visitTry(aTry: IrTry) {
        visitElement(aTry)
    }

    final override fun visitTry(aTry: IrTry, data: Nothing?) {
        visitTry(aTry)
    }

    open fun visitCatch(aCatch: IrCatch) {
        visitElement(aCatch)
    }

    final override fun visitCatch(aCatch: IrCatch, data: Nothing?) {
        visitCatch(aCatch)
    }

    open fun visitBreak(jump: IrBreak) {
        visitElement(jump)
    }

    final override fun visitBreak(jump: IrBreak, data: Nothing?) {
        visitBreak(jump)
    }

    open fun visitContinue(jump: IrContinue) {
        visitElement(jump)
    }

    final override fun visitContinue(jump: IrContinue, data: Nothing?) {
        visitContinue(jump)
    }

    open fun visitReturn(expression: IrReturn) {
        visitElement(expression)
    }

    final override fun visitReturn(expression: IrReturn, data: Nothing?) {
        visitReturn(expression)
    }

    open fun visitThrow(expression: IrThrow) {
        visitElement(expression)
    }

    final override fun visitThrow(expression: IrThrow, data: Nothing?) {
        visitThrow(expression)
    }

    open fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression) {
        visitElement(expression)
    }

    final override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: Nothing?) {
        visitDynamicOperatorExpression(expression)
    }

    open fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression) {
        visitElement(expression)
    }

    final override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: Nothing?) {
        visitDynamicMemberExpression(expression)
    }

    open fun visitErrorDeclaration(declaration: IrErrorDeclaration) {
        visitElement(declaration)
    }

    final override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?) {
        visitErrorDeclaration(declaration)
    }

    open fun visitErrorExpression(expression: IrErrorExpression) {
        visitElement(expression)
    }

    final override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?) {
        visitErrorExpression(expression)
    }

    open fun visitErrorCallExpression(expression: IrErrorCallExpression) {
        visitElement(expression)
    }

    final override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?) {
        visitErrorCallExpression(expression)
    }
}
