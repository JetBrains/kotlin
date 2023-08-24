/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstantArray
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrConstantPrimitive
import org.jetbrains.kotlin.ir.expressions.IrConstantValue
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrDynamicExpression
import org.jetbrains.kotlin.ir.expressions.IrDynamicMemberExpression
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperatorExpression
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrErrorCallExpression
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetSingletonValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrRawFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrSuspendableExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop

abstract class IrElementTransformerShallow<in D> : IrElementVisitor<IrElement, D>() {
    override fun visitElement(element: IrElement, data: D): IrElement {
        element.transformChildren(this, data)
        return element
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitClass(declaration: IrClass, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: D):
            IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitFunction(declaration: IrFunction, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitConstructor(declaration: IrConstructor, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: D):
            IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitField(declaration: IrField, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty,
            data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: D):
            IrModuleFragment {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitProperty(declaration: IrProperty, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitScript(declaration: IrScript, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitVariable(declaration: IrVariable, data: D): IrStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitPackageFragment(declaration: IrPackageFragment, data: D): IrElement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment,
            data: D): IrExternalPackageFragment {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitFile(declaration: IrFile, data: D): IrFile {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitExpression(expression: IrExpression, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitBody(body: IrBody, data: D): IrBody {
        body.transformChildren(this, data)
        return body
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: D): IrBody {
        body.transformChildren(this, data)
        return body
    }

    override fun visitBlockBody(body: IrBlockBody, data: D): IrBody {
        body.transformChildren(this, data)
        return body
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: D):
            IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: D):
            IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: D): IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitSingletonReference(expression: IrGetSingletonValue, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitBlock(expression: IrBlock, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitComposite(expression: IrComposite, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: D): IrBody {
        body.transformChildren(this, data)
        return body
    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: D): IrExpression {
        jump.transformChildren(this, data)
        return jump
    }

    override fun visitBreak(jump: IrBreak, data: D): IrExpression {
        jump.transformChildren(this, data)
        return jump
    }

    override fun visitContinue(jump: IrContinue, data: D): IrExpression {
        jump.transformChildren(this, data)
        return jump
    }

    override fun visitCall(expression: IrCall, data: D): IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitCallableReference(expression: IrCallableReference<*>, data: D):
            IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: D):
            IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitPropertyReference(expression: IrPropertyReference, data: D):
            IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override
            fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference,
            data: D): IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitClassReference(expression: IrClassReference, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitConst(expression: IrConst<*>, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitConstantValue(expression: IrConstantValue, data: D):
            IrConstantValue {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitConstantPrimitive(expression: IrConstantPrimitive, data: D):
            IrConstantValue {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitConstantObject(expression: IrConstantObject, data: D):
            IrConstantValue {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitConstantArray(expression: IrConstantArray, data: D):
            IrConstantValue {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall,
            data: D): IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitDynamicExpression(expression: IrDynamicExpression, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression,
            data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression,
            data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: D):
            IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitErrorExpression(expression: IrErrorExpression, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitGetField(expression: IrGetField, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitSetField(expression: IrSetField, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: D):
            IrElement {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitGetClass(expression: IrGetClass, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall,
            data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitLoop(loop: IrLoop, data: D): IrExpression {
        loop.transformChildren(this, data)
        return loop
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: D): IrExpression {
        loop.transformChildren(this, data)
        return loop
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: D): IrExpression {
        loop.transformChildren(this, data)
        return loop
    }

    override fun visitReturn(expression: IrReturn, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitSuspendableExpression(expression: IrSuspendableExpression, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitThrow(expression: IrThrow, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitTry(aTry: IrTry, data: D): IrExpression {
        aTry.transformChildren(this, data)
        return aTry
    }

    override fun visitCatch(aCatch: IrCatch, data: D): IrCatch {
        aCatch.transformChildren(this, data)
        return aCatch
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitValueAccess(expression: IrValueAccessExpression, data: D):
            IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitGetValue(expression: IrGetValue, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitSetValue(expression: IrSetValue, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitVararg(expression: IrVararg, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: D): IrSpreadElement {
        spread.transformChildren(this, data)
        return spread
    }

    override fun visitWhen(expression: IrWhen, data: D): IrExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitBranch(branch: IrBranch, data: D): IrBranch {
        branch.transformChildren(this, data)
        return branch
    }

    override fun visitElseBranch(branch: IrElseBranch, data: D): IrElseBranch {
        branch.transformChildren(this, data)
        return branch
    }
}
