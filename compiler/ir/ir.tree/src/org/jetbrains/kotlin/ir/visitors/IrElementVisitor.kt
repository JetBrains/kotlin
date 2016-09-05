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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*

interface IrElementVisitor<out R, in D> {
    fun visitElement(element: IrElement, data: D): R
    fun visitModule(declaration: IrModule, data: D) = visitElement(declaration, data)
    fun visitFile(declaration: IrFile, data: D) = visitElement(declaration, data)

    fun visitDeclaration(declaration: IrDeclaration, data: D) = visitElement(declaration, data)
    fun visitClass(declaration: IrClass, data: D) = visitDeclaration(declaration, data)
    fun visitTypeAlias(declaration: IrTypeAlias, data: D) = visitDeclaration(declaration, data)
    fun visitGeneralFunction(declaration: IrGeneralFunction, data: D) = visitDeclaration(declaration, data)
    fun visitFunction(declaration: IrFunction, data: D) = visitGeneralFunction(declaration, data)
    fun visitPropertyGetter(declaration: IrPropertyGetter, data: D) = visitGeneralFunction(declaration, data)
    fun visitPropertySetter(declaration: IrPropertySetter, data: D) = visitGeneralFunction(declaration, data)
    fun visitConstructor(declaration: IrConstructor, data: D) = visitGeneralFunction(declaration, data)
    fun visitProperty(declaration: IrProperty, data: D) = visitDeclaration(declaration, data)
    fun visitSimpleProperty(declaration: IrSimpleProperty, data: D) = visitProperty(declaration, data)
    fun visitDelegatedProperty(declaration: IrDelegatedProperty, data: D) = visitProperty(declaration, data)
    fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: D) = visitDeclaration(declaration, data)
    fun visitLocalPropertyAccessor(declaration: IrLocalPropertyAccessor, data: D) = visitGeneralFunction(declaration, data)
    fun visitVariable(declaration: IrVariable, data: D) = visitDeclaration(declaration, data)
    fun visitEnumEntry(declaration: IrEnumEntry, data: D) = visitDeclaration(declaration, data)
    fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: D) = visitDeclaration(declaration, data)

    fun visitBody(body: IrBody, data: D) = visitElement(body, data)
    fun visitExpressionBody(body: IrExpressionBody, data: D) = visitBody(body, data)
    fun visitBlockBody(body: IrBlockBody, data: D) = visitBody(body, data)
    fun visitSyntheticBody(body: IrSyntheticBody, data: D) = visitBody(body, data)

    fun visitExpression(expression: IrExpression, data: D) = visitElement(expression, data)
    fun <T> visitConst(expression: IrConst<T>, data: D) = visitExpression(expression, data)
    fun visitVararg(expression: IrVararg, data: D) = visitExpression(expression, data)
    fun visitSpreadElement(spread: IrSpreadElement, data: D) = visitElement(spread, data)

    fun visitBlock(expression: IrBlock, data: D) = visitExpression(expression, data)
    fun visitStringConcatenation(expression: IrStringConcatenation, data: D) = visitExpression(expression, data)
    fun visitThisReference(expression: IrThisReference, data: D) = visitExpression(expression, data)

    fun visitDeclarationReference(expression: IrDeclarationReference, data: D) = visitExpression(expression, data)
    fun visitSingletonReference(expression: IrGetSingletonValue, data: D) = visitDeclarationReference(expression, data)
    fun visitGetObjectValue(expression: IrGetObjectValue, data: D) = visitSingletonReference(expression, data)
    fun visitGetEnumValue(expression: IrGetEnumValue, data: D) = visitSingletonReference(expression, data)
    fun visitGetVariable(expression: IrGetVariable, data: D) = visitDeclarationReference(expression, data)
    fun visitSetVariable(expression: IrSetVariable, data: D) = visitDeclarationReference(expression, data)
    fun visitGetBackingField(expression: IrGetBackingField, data: D) = visitDeclarationReference(expression, data)
    fun visitSetBackingField(expression: IrSetBackingField, data: D) = visitDeclarationReference(expression, data)
    fun visitGetExtensionReceiver(expression: IrGetExtensionReceiver, data: D) = visitDeclarationReference(expression, data)
    fun visitGeneralCall(expression: IrGeneralCall, data: D) = visitDeclarationReference(expression, data)
    fun visitCall(expression: IrCall, data: D) = visitGeneralCall(expression, data)
    fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: D) = visitGeneralCall(expression, data)
    fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: D) = visitGeneralCall(expression, data)
    fun visitGetClass(expression: IrGetClass, data: D) = visitExpression(expression, data)

    fun visitCallableReference(expression: IrCallableReference, data: D) = visitGeneralCall(expression, data)
    fun visitClassReference(expression: IrClassReference, data: D) = visitDeclarationReference(expression, data)

    fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: D) = visitExpression(expression, data)

    fun visitTypeOperator(expression: IrTypeOperatorCall, data: D) = visitExpression(expression, data)

    fun visitWhen(expression: IrWhen, data: D) = visitExpression(expression, data)
    fun visitLoop(loop: IrLoop, data: D) = visitExpression(loop, data)
    fun visitWhileLoop(loop: IrWhileLoop, data: D) = visitLoop(loop, data)
    fun visitDoWhileLoop(loop: IrDoWhileLoop, data: D) = visitLoop(loop, data)
    fun visitTryCatch(tryCatch: IrTryCatch, data: D) = visitExpression(tryCatch, data)

    fun visitBreakContinue(jump: IrBreakContinue, data: D) = visitExpression(jump, data)
    fun visitBreak(jump: IrBreak, data: D) = visitBreakContinue(jump, data)
    fun visitContinue(jump: IrContinue, data: D) = visitBreakContinue(jump, data)

    fun visitReturn(expression: IrReturn, data: D) = visitExpression(expression, data)
    fun visitThrow(expression: IrThrow, data: D) = visitExpression(expression, data)

    fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: D) = visitDeclaration(declaration, data)
    fun visitErrorExpression(expression: IrErrorExpression, data: D) = visitExpression(expression, data)
    fun visitErrorCallExpression(expression: IrErrorCallExpression, data: D) = visitErrorExpression(expression, data)
}
