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

abstract class IrElementTransformerVoid : IrElementTransformerVoidShallow() {
    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        declaration.transformChildren(this, null)
        return declaration
    }

    override fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment {
        declaration.transformChildren(this, null)
        return declaration
    }

    override fun visitFile(declaration: IrFile): IrFile = visitPackageFragment(declaration) as IrFile
    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment): IrExternalPackageFragment =
        visitPackageFragment(declaration) as IrExternalPackageFragment

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        declaration.transformChildren(this, null)
        return declaration
    }

    override fun visitScript(declaration: IrScript) = visitDeclaration(declaration)
    override fun visitClass(declaration: IrClass) = visitDeclaration(declaration)
    override fun visitFunction(declaration: IrFunction) = visitDeclaration(declaration)
    override fun visitSimpleFunction(declaration: IrSimpleFunction) = visitFunction(declaration)
    override fun visitConstructor(declaration: IrConstructor) = visitFunction(declaration)
    override fun visitProperty(declaration: IrProperty) = visitDeclaration(declaration)
    override fun visitField(declaration: IrField) = visitDeclaration(declaration)
    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) = visitDeclaration(declaration)

    override fun visitEnumEntry(declaration: IrEnumEntry) = visitDeclaration(declaration)
    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) = visitDeclaration(declaration)
    override fun visitTypeParameter(declaration: IrTypeParameter) = visitDeclaration(declaration)
    override fun visitValueParameter(declaration: IrValueParameter) = visitDeclaration(declaration)
    override fun visitVariable(declaration: IrVariable) = visitDeclaration(declaration)
    override fun visitTypeAlias(declaration: IrTypeAlias) = visitDeclaration(declaration)
    override fun visitBody(body: IrBody): IrBody {
        body.transformChildren(this, null)
        return body
    }

    override fun visitExpressionBody(body: IrExpressionBody) = visitBody(body)
    override fun visitBlockBody(body: IrBlockBody) = visitBody(body)
    override fun visitSyntheticBody(body: IrSyntheticBody) = visitBody(body)
    override fun visitSuspendableExpression(expression: IrSuspendableExpression) = visitExpression(expression)
    override fun visitSuspensionPoint(expression: IrSuspensionPoint) = visitExpression(expression)
    override fun visitExpression(expression: IrExpression): IrExpression {
        expression.transformChildren(this, null)
        return expression
    }

    override fun visitConst(expression: IrConst<*>) = visitExpression(expression)
    override fun visitConstantValue(expression: IrConstantValue): IrConstantValue {
        expression.transformChildren(this, null)
        return expression
    }

    override fun visitConstantObject(expression: IrConstantObject) = visitConstantValue(expression)
    override fun visitConstantPrimitive(expression: IrConstantPrimitive) = visitConstantValue(expression)
    override fun visitConstantArray(expression: IrConstantArray) = visitConstantValue(expression)
    override fun visitVararg(expression: IrVararg) = visitExpression(expression)
    override fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement {
        spread.transformChildren(this, null)
        return spread
    }

    override fun visitContainerExpression(expression: IrContainerExpression) = visitExpression(expression)
    override fun visitBlock(expression: IrBlock) = visitContainerExpression(expression)
    override fun visitComposite(expression: IrComposite) = visitContainerExpression(expression)
    override fun visitStringConcatenation(expression: IrStringConcatenation) = visitExpression(expression)
    override fun visitDeclarationReference(expression: IrDeclarationReference) = visitExpression(expression)
    override fun visitSingletonReference(expression: IrGetSingletonValue) = visitDeclarationReference(expression)
    override fun visitGetObjectValue(expression: IrGetObjectValue) = visitSingletonReference(expression)
    override fun visitGetEnumValue(expression: IrGetEnumValue) = visitSingletonReference(expression)
    override fun visitValueAccess(expression: IrValueAccessExpression) = visitDeclarationReference(expression)
    override fun visitGetValue(expression: IrGetValue) = visitValueAccess(expression)
    override fun visitSetValue(expression: IrSetValue) = visitValueAccess(expression)
    override fun visitFieldAccess(expression: IrFieldAccessExpression) = visitDeclarationReference(expression)
    override fun visitGetField(expression: IrGetField) = visitFieldAccess(expression)
    override fun visitSetField(expression: IrSetField) = visitFieldAccess(expression)
    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) = visitDeclarationReference(expression)
    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) = visitMemberAccess(expression)
    override fun visitCall(expression: IrCall) = visitFunctionAccess(expression)
    override fun visitConstructorCall(expression: IrConstructorCall) = visitFunctionAccess(expression)
    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) = visitFunctionAccess(expression)
    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) = visitFunctionAccess(expression)
    override fun visitGetClass(expression: IrGetClass) = visitExpression(expression)
    override fun visitCallableReference(expression: IrCallableReference<*>) = visitMemberAccess(expression)
    override fun visitFunctionReference(expression: IrFunctionReference) = visitCallableReference(expression)
    override fun visitPropertyReference(expression: IrPropertyReference) = visitCallableReference(expression)
    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) = visitCallableReference(expression)
    override fun visitRawFunctionReference(expression: IrRawFunctionReference) = visitDeclarationReference(expression)
    override fun visitFunctionExpression(expression: IrFunctionExpression) = visitExpression(expression)
    override fun visitClassReference(expression: IrClassReference) = visitDeclarationReference(expression)
    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) = visitExpression(expression)
    override fun visitTypeOperator(expression: IrTypeOperatorCall) = visitExpression(expression)
    override fun visitWhen(expression: IrWhen) = visitExpression(expression)
    override fun visitBranch(branch: IrBranch): IrBranch {
        branch.transformChildren(this, null)
        return branch
    }

    override fun visitElseBranch(branch: IrElseBranch): IrElseBranch {
        branch.transformChildren(this, null)
        return branch
    }

    override fun visitLoop(loop: IrLoop) = visitExpression(loop)
    override fun visitWhileLoop(loop: IrWhileLoop) = visitLoop(loop)
    override fun visitDoWhileLoop(loop: IrDoWhileLoop) = visitLoop(loop)
    override fun visitTry(aTry: IrTry) = visitExpression(aTry)
    override fun visitCatch(aCatch: IrCatch): IrCatch {
        aCatch.transformChildren(this, null)
        return aCatch
    }

    override fun visitBreakContinue(jump: IrBreakContinue) = visitExpression(jump)
    override fun visitBreak(jump: IrBreak) = visitBreakContinue(jump)
    override fun visitContinue(jump: IrContinue) = visitBreakContinue(jump)
    override fun visitReturn(expression: IrReturn) = visitExpression(expression)
    override fun visitThrow(expression: IrThrow) = visitExpression(expression)
    override fun visitDynamicExpression(expression: IrDynamicExpression) = visitExpression(expression)
    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression) = visitDynamicExpression(expression)
    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression) = visitDynamicExpression(expression)
    override fun visitErrorDeclaration(declaration: IrErrorDeclaration) = visitDeclaration(declaration)
    override fun visitErrorExpression(expression: IrErrorExpression) = visitExpression(expression)
    override fun visitErrorCallExpression(expression: IrErrorCallExpression) = visitErrorExpression(expression)
}

fun IrElement.transformChildrenVoid(transformer: IrElementTransformerVoid) {
    transformChildren(transformer, null)
}