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
    fun visitModule(declaration: IrModule, data: D): R = visitElement(declaration, data)
    fun visitFile(declaration: IrFile, data: D): R = visitElement(declaration, data)

    fun visitDeclaration(declaration: IrDeclaration, data: D): R = visitElement(declaration, data)
    fun visitClass(declaration: IrClass, data: D): R = visitDeclaration(declaration, data)
    fun visitFunction(declaration: IrFunction, data: D): R = visitDeclaration(declaration, data)
    fun visitPropertyGetter(declaration: IrPropertyGetter, data: D): R = visitFunction(declaration, data)
    fun visitPropertySetter(declaration: IrPropertySetter, data: D): R = visitFunction(declaration, data)
    fun visitProperty(declaration: IrProperty, data: D): R = visitDeclaration(declaration, data)
    fun visitSimpleProperty(declaration: IrSimpleProperty, data: D): R = visitProperty(declaration, data)
    fun visitDelegatedProperty(declaration: IrDelegatedProperty, data: D): R = visitProperty(declaration, data)
    fun visitLocalVariable(declaration: IrVariable, data: D) = visitDeclaration(declaration, data)


    fun visitBody(body: IrBody, data: D): R = visitElement(body, data)
    fun visitExpressionBody(body: IrExpressionBody, data: D): R = visitBody(body, data)

    fun visitExpression(expression: IrExpression, data: D): R = visitElement(expression, data)
    fun <T> visitLiteral(expression: IrLiteralExpression<T>, data: D): R = visitExpression(expression, data)
    fun visitReturnExpression(expression: IrReturnExpression, data: D): R = visitExpression(expression, data)
    fun visitBlockExpression(expression: IrBlockExpression, data: D): R = visitExpression(expression, data)
    fun visitStringTemplate(expression: IrStringConcatenationExpression, data: D) = visitExpression(expression, data)
    fun visitThisExpression(expression: IrThisExpression, data: D) = visitExpression(expression, data)

    fun visitDeclarationReference(expression: IrDeclarationReference, data: D) = visitExpression(expression, data)
    fun visitGetObjectValue(expression: IrGetObjectValueExpression, data: D) = visitDeclarationReference(expression, data)
    fun visitGetEnumValue(expression: IrGetEnumValueExpression, data: D) = visitDeclarationReference(expression, data)
    fun visitGetVariable(expression: IrGetVariableExpression, data: D) = visitDeclarationReference(expression, data)
    fun visitSetVariable(expression: IrSetVariableExpression, data: D) = visitDeclarationReference(expression, data)
    fun visitGetExtensionReceiver(expression: IrGetExtensionReceiverExpression, data: D) = visitDeclarationReference(expression, data)
    fun visitMemberAccess(expression: IrMemberAccessExpression, data: D) = visitDeclarationReference(expression, data)
    fun visitCallExpression(expression: IrCallExpression, data: D) = visitMemberAccess(expression, data)
    fun visitPropertyAccess(expression: IrPropertyAccessExpression, data: D) = visitMemberAccess(expression, data)
    fun visitGetProperty(expression: IrGetPropertyExpression, data: D) = visitPropertyAccess(expression, data)
    fun visitSetProperty(expression: IrSetPropertyExpression, data: D) = visitPropertyAccess(expression, data)

    fun visitOperatorExpression(expression: IrOperatorExpression, data: D) = visitExpression(expression, data)
    fun visitUnaryOperator(expression: IrUnaryOperatorExpression, data: D) = visitOperatorExpression(expression, data)
    fun visitBinaryOperator(expression: IrBinaryOperatorExpression, data: D) = visitOperatorExpression(expression, data)
    fun visitTypeOperatorExpression(expression: IrTypeOperatorExpression, data: D) = visitExpression(expression, data)

    // NB Use it only for testing purposes; will be removed as soon as all Kotlin expression types are covered
    fun visitDummyDeclaration(declaration: IrDummyDeclaration, data: D) = visitDeclaration(declaration, data)
    fun visitDummyExpression(expression: IrDummyExpression, data: D) = visitExpression(expression, data)

}
