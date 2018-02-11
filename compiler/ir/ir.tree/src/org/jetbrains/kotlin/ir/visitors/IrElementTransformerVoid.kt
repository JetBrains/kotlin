/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

abstract class IrElementTransformerVoid : IrElementTransformer<Nothing?> {
    protected fun <T : IrElement> T.transformChildren() = apply { transformChildrenVoid() }

    open fun visitElement(element: IrElement): IrElement = element.transformChildren()
    override final fun visitElement(element: IrElement, data: Nothing?): IrElement = visitElement(element)

    open fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment = declaration.transformChildren()
    override final fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): IrModuleFragment =
        visitModuleFragment(declaration)

    open fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment = declaration.transformChildren()
    override fun visitPackageFragment(declaration: IrPackageFragment, data: Nothing?): IrElement = visitPackageFragment(declaration)

    open fun visitFile(declaration: IrFile): IrFile = visitPackageFragment(declaration) as IrFile
    override final fun visitFile(declaration: IrFile, data: Nothing?): IrFile = visitFile(declaration)

    open fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) =
        visitPackageFragment(declaration) as IrExternalPackageFragment

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): IrExternalPackageFragment =
        visitExternalPackageFragment(declaration)

    open fun visitDeclaration(declaration: IrDeclaration): IrStatement = declaration.transformChildren()
    override final fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): IrStatement = visitDeclaration(declaration)

    open fun visitClass(declaration: IrClass) = visitDeclaration(declaration)
    override final fun visitClass(declaration: IrClass, data: Nothing?) = visitClass(declaration)

    open fun visitTypeAlias(declaration: IrTypeAlias) = visitDeclaration(declaration)
    override final fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?) = visitTypeAlias(declaration)

    open fun visitFunction(declaration: IrFunction) = visitDeclaration(declaration)
    override final fun visitFunction(declaration: IrFunction, data: Nothing?) = visitFunction(declaration)

    open fun visitSimpleFunction(declaration: IrSimpleFunction) = visitFunction(declaration)
    override final fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?) = visitSimpleFunction(declaration)

    open fun visitConstructor(declaration: IrConstructor) = visitFunction(declaration)
    override final fun visitConstructor(declaration: IrConstructor, data: Nothing?) = visitConstructor(declaration)

    open fun visitProperty(declaration: IrProperty) = visitDeclaration(declaration)
    override final fun visitProperty(declaration: IrProperty, data: Nothing?) = visitProperty(declaration)

    open fun visitField(declaration: IrField) = visitDeclaration(declaration)
    override final fun visitField(declaration: IrField, data: Nothing?) = visitField(declaration)

    open fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) = visitDeclaration(declaration)
    override final fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?) =
        visitLocalDelegatedProperty(declaration)

    open fun visitEnumEntry(declaration: IrEnumEntry) = visitDeclaration(declaration)
    override final fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?) = visitEnumEntry(declaration)

    open fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) = visitDeclaration(declaration)
    override final fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?) =
        visitAnonymousInitializer(declaration)

    open fun visitTypeParameter(declaration: IrTypeParameter) = visitDeclaration(declaration)
    override final fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): IrStatement = visitTypeParameter(declaration)

    open fun visitValueParameter(declaration: IrValueParameter) = visitDeclaration(declaration)
    override final fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): IrStatement = visitValueParameter(declaration)

    open fun visitVariable(declaration: IrVariable) = visitDeclaration(declaration)
    override final fun visitVariable(declaration: IrVariable, data: Nothing?) = visitVariable(declaration)

    open fun visitBody(body: IrBody): IrBody = body.transformChildren()
    override final fun visitBody(body: IrBody, data: Nothing?): IrBody = visitBody(body)

    open fun visitExpressionBody(body: IrExpressionBody) = visitBody(body)
    override final fun visitExpressionBody(body: IrExpressionBody, data: Nothing?) = visitExpressionBody(body)

    open fun visitBlockBody(body: IrBlockBody) = visitBody(body)
    override final fun visitBlockBody(body: IrBlockBody, data: Nothing?) = visitBlockBody(body)

    open fun visitSyntheticBody(body: IrSyntheticBody) = visitBody(body)
    override final fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?) = visitSyntheticBody(body)

    open fun visitExpression(expression: IrExpression): IrExpression = expression.transformChildren()
    override final fun visitExpression(expression: IrExpression, data: Nothing?): IrExpression = visitExpression(expression)

    open fun <T> visitConst(expression: IrConst<T>) = visitExpression(expression)
    override final fun <T> visitConst(expression: IrConst<T>, data: Nothing?) = visitConst(expression)

    open fun visitVararg(expression: IrVararg) = visitExpression(expression)
    override final fun visitVararg(expression: IrVararg, data: Nothing?) = visitVararg(expression)

    open fun visitSpreadElement(spread: IrSpreadElement) = spread.transformChildren()
    override final fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): IrSpreadElement = visitSpreadElement(spread)

    open fun visitContainerExpression(expression: IrContainerExpression) = visitExpression(expression)
    override final fun visitContainerExpression(expression: IrContainerExpression, data: Nothing?) = visitContainerExpression(expression)

    open fun visitBlock(expression: IrBlock) = visitContainerExpression(expression)
    override final fun visitBlock(expression: IrBlock, data: Nothing?) = visitBlock(expression)

    open fun visitComposite(expression: IrComposite) = visitContainerExpression(expression)
    override final fun visitComposite(expression: IrComposite, data: Nothing?) = visitComposite(expression)

    open fun visitStringConcatenation(expression: IrStringConcatenation) = visitExpression(expression)
    override final fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?) = visitStringConcatenation(expression)

    open fun visitDeclarationReference(expression: IrDeclarationReference) = visitExpression(expression)
    override final fun visitDeclarationReference(expression: IrDeclarationReference, data: Nothing?) = visitDeclarationReference(expression)

    open fun visitSingletonReference(expression: IrGetSingletonValue) = visitDeclarationReference(expression)
    override final fun visitSingletonReference(expression: IrGetSingletonValue, data: Nothing?) = visitSingletonReference(expression)

    open fun visitGetObjectValue(expression: IrGetObjectValue) = visitSingletonReference(expression)
    override final fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?) = visitGetObjectValue(expression)

    open fun visitGetEnumValue(expression: IrGetEnumValue) = visitSingletonReference(expression)
    override final fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?) = visitGetEnumValue(expression)

    open fun visitValueAccess(expression: IrValueAccessExpression) = visitDeclarationReference(expression)
    override final fun visitValueAccess(expression: IrValueAccessExpression, data: Nothing?) = visitValueAccess(expression)

    open fun visitGetValue(expression: IrGetValue) = visitValueAccess(expression)
    override final fun visitGetValue(expression: IrGetValue, data: Nothing?) = visitGetValue(expression)

    open fun visitSetVariable(expression: IrSetVariable) = visitValueAccess(expression)
    override final fun visitSetVariable(expression: IrSetVariable, data: Nothing?) = visitSetVariable(expression)

    open fun visitFieldAccess(expression: IrFieldAccessExpression) = visitDeclarationReference(expression)
    override final fun visitFieldAccess(expression: IrFieldAccessExpression, data: Nothing?) = visitFieldAccess(expression)

    open fun visitGetField(expression: IrGetField) = visitFieldAccess(expression)
    override final fun visitGetField(expression: IrGetField, data: Nothing?) = visitGetField(expression)

    open fun visitSetField(expression: IrSetField) = visitFieldAccess(expression)
    override final fun visitSetField(expression: IrSetField, data: Nothing?) = visitSetField(expression)

    open fun visitMemberAccess(expression: IrMemberAccessExpression) = visitExpression(expression)
    override final fun visitMemberAccess(expression: IrMemberAccessExpression, data: Nothing?) = visitMemberAccess(expression)

    open fun visitFunctionAccess(expression: IrFunctionAccessExpression) = visitMemberAccess(expression)
    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Nothing?) = visitFunctionAccess(expression)

    open fun visitCall(expression: IrCall) = visitFunctionAccess(expression)
    override final fun visitCall(expression: IrCall, data: Nothing?) = visitCall(expression)

    open fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) = visitMemberAccess(expression)
    override final fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?) =
        visitDelegatingConstructorCall(expression)

    open fun visitEnumConstructorCall(expression: IrEnumConstructorCall) = visitMemberAccess(expression)
    override final fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?) = visitEnumConstructorCall(expression)

    open fun visitGetClass(expression: IrGetClass) = visitExpression(expression)
    override final fun visitGetClass(expression: IrGetClass, data: Nothing?) = visitGetClass(expression)

    open fun visitCallableReference(expression: IrCallableReference) = visitMemberAccess(expression)
    override final fun visitCallableReference(expression: IrCallableReference, data: Nothing?) = visitCallableReference(expression)

    open fun visitFunctionReference(expression: IrFunctionReference) = visitCallableReference(expression)
    override final fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): IrElement =
        visitFunctionReference(expression)

    open fun visitPropertyReference(expression: IrPropertyReference) = visitCallableReference(expression)
    override final fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): IrElement =
        visitPropertyReference(expression)

    open fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) = visitCallableReference(expression)
    override final fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: Nothing?) =
        visitLocalDelegatedPropertyReference(expression)

    open fun visitClassReference(expression: IrClassReference) = visitDeclarationReference(expression)
    override final fun visitClassReference(expression: IrClassReference, data: Nothing?) = visitClassReference(expression)

    open fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) = visitExpression(expression)
    override final fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?) =
        visitInstanceInitializerCall(expression)

    open fun visitTypeOperator(expression: IrTypeOperatorCall) = visitExpression(expression)
    override final fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?) = visitTypeOperator(expression)

    open fun visitWhen(expression: IrWhen) = visitExpression(expression)
    override final fun visitWhen(expression: IrWhen, data: Nothing?) = visitWhen(expression)

    open fun visitBranch(branch: IrBranch) = branch.transformChildren()
    override final fun visitBranch(branch: IrBranch, data: Nothing?): IrBranch = visitBranch(branch)

    open fun visitElseBranch(branch: IrElseBranch) = branch.transformChildren()
    override final fun visitElseBranch(branch: IrElseBranch, data: Nothing?): IrElseBranch = visitElseBranch(branch)

    open fun visitLoop(loop: IrLoop) = visitExpression(loop)
    override final fun visitLoop(loop: IrLoop, data: Nothing?) = visitLoop(loop)

    open fun visitWhileLoop(loop: IrWhileLoop) = visitLoop(loop)
    override final fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?) = visitWhileLoop(loop)

    open fun visitDoWhileLoop(loop: IrDoWhileLoop) = visitLoop(loop)
    override final fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?) = visitDoWhileLoop(loop)

    open fun visitTry(aTry: IrTry) = visitExpression(aTry)
    override final fun visitTry(aTry: IrTry, data: Nothing?) = visitTry(aTry)

    open fun visitCatch(aCatch: IrCatch): IrCatch = aCatch.apply { transformChildrenVoid() }
    override final fun visitCatch(aCatch: IrCatch, data: Nothing?): IrCatch = visitCatch(aCatch)

    open fun visitBreakContinue(jump: IrBreakContinue) = visitExpression(jump)
    override final fun visitBreakContinue(jump: IrBreakContinue, data: Nothing?) = visitBreakContinue(jump)

    open fun visitBreak(jump: IrBreak) = visitBreakContinue(jump)
    override final fun visitBreak(jump: IrBreak, data: Nothing?) = visitBreak(jump)

    open fun visitContinue(jump: IrContinue) = visitBreakContinue(jump)
    override final fun visitContinue(jump: IrContinue, data: Nothing?) = visitContinue(jump)

    open fun visitReturn(expression: IrReturn) = visitExpression(expression)
    override final fun visitReturn(expression: IrReturn, data: Nothing?) = visitReturn(expression)

    open fun visitThrow(expression: IrThrow) = visitExpression(expression)
    override final fun visitThrow(expression: IrThrow, data: Nothing?) = visitThrow(expression)

    open fun visitErrorDeclaration(declaration: IrErrorDeclaration) = visitDeclaration(declaration)
    override final fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?) = visitErrorDeclaration(declaration)

    open fun visitErrorExpression(expression: IrErrorExpression) = visitExpression(expression)
    override final fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?) = visitErrorExpression(expression)

    open fun visitErrorCallExpression(expression: IrErrorCallExpression) = visitErrorExpression(expression)
    override final fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?) = visitErrorCallExpression(expression)

    protected inline fun <T : IrElement> T.transformPostfix(body: T.() -> Unit): T {
        transformChildrenVoid()
        this.body()
        return this
    }

    protected fun IrElement.transformChildrenVoid() {
        transformChildrenVoid(this@IrElementTransformerVoid)
    }
}

fun IrElement.transformChildrenVoid(transformer: IrElementTransformerVoid) {
    transformChildren(transformer, null)
}