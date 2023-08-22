/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
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

abstract class IrElementVisitor<out R, in D> {
    abstract fun visitElement(element: IrElement, data: D): R

    open fun visitDeclaration(declaration: IrDeclarationBase, data: D): R =
            visitElement(declaration, data)

    open fun visitValueParameter(declaration: IrValueParameter, data: D): R =
            visitDeclaration(declaration, data)

    open fun visitClass(declaration: IrClass, data: D): R = visitDeclaration(declaration,
            data)

    open fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: D): R =
            visitDeclaration(declaration, data)

    open fun visitTypeParameter(declaration: IrTypeParameter, data: D): R =
            visitDeclaration(declaration, data)

    open fun visitFunction(declaration: IrFunction, data: D): R =
            visitDeclaration(declaration, data)

    open fun visitConstructor(declaration: IrConstructor, data: D): R =
            visitFunction(declaration, data)

    open fun visitEnumEntry(declaration: IrEnumEntry, data: D): R =
            visitDeclaration(declaration, data)

    open fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: D): R =
            visitDeclaration(declaration, data)

    open fun visitField(declaration: IrField, data: D): R = visitDeclaration(declaration,
            data)

    open fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: D): R
            = visitDeclaration(declaration, data)

    open fun visitModuleFragment(declaration: IrModuleFragment, data: D): R =
            visitElement(declaration, data)

    open fun visitProperty(declaration: IrProperty, data: D): R =
            visitDeclaration(declaration, data)

    open fun visitScript(declaration: IrScript, data: D): R = visitDeclaration(declaration,
            data)

    open fun visitSimpleFunction(declaration: IrSimpleFunction, data: D): R =
            visitFunction(declaration, data)

    open fun visitTypeAlias(declaration: IrTypeAlias, data: D): R =
            visitDeclaration(declaration, data)

    open fun visitVariable(declaration: IrVariable, data: D): R =
            visitDeclaration(declaration, data)

    open fun visitPackageFragment(declaration: IrPackageFragment, data: D): R =
            visitElement(declaration, data)

    open fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: D):
            R = visitPackageFragment(declaration, data)

    open fun visitFile(declaration: IrFile, data: D): R = visitPackageFragment(declaration,
            data)

    open fun visitExpression(expression: IrExpression, data: D): R =
            visitElement(expression, data)

    open fun visitBody(body: IrBody, data: D): R = visitElement(body, data)

    open fun visitExpressionBody(body: IrExpressionBody, data: D): R = visitBody(body,
            data)

    open fun visitBlockBody(body: IrBlockBody, data: D): R = visitBody(body, data)

    open fun visitDeclarationReference(expression: IrDeclarationReference, data: D): R =
            visitExpression(expression, data)

    open fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: D): R =
            visitDeclarationReference(expression, data)

    open fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: D): R =
            visitMemberAccess(expression, data)

    open fun visitConstructorCall(expression: IrConstructorCall, data: D): R =
            visitFunctionAccess(expression, data)

    open fun visitSingletonReference(expression: IrGetSingletonValue, data: D): R =
            visitDeclarationReference(expression, data)

    open fun visitGetObjectValue(expression: IrGetObjectValue, data: D): R =
            visitSingletonReference(expression, data)

    open fun visitGetEnumValue(expression: IrGetEnumValue, data: D): R =
            visitSingletonReference(expression, data)

    open fun visitRawFunctionReference(expression: IrRawFunctionReference, data: D): R =
            visitDeclarationReference(expression, data)

    open fun visitContainerExpression(expression: IrContainerExpression, data: D): R =
            visitExpression(expression, data)

    open fun visitBlock(expression: IrBlock, data: D): R =
            visitContainerExpression(expression, data)

    open fun visitComposite(expression: IrComposite, data: D): R =
            visitContainerExpression(expression, data)

    open fun visitSyntheticBody(body: IrSyntheticBody, data: D): R = visitBody(body, data)

    open fun visitBreakContinue(jump: IrBreakContinue, data: D): R = visitExpression(jump,
            data)

    open fun visitBreak(jump: IrBreak, data: D): R = visitBreakContinue(jump, data)

    open fun visitContinue(jump: IrContinue, data: D): R = visitBreakContinue(jump, data)

    open fun visitCall(expression: IrCall, data: D): R = visitFunctionAccess(expression,
            data)

    open fun visitCallableReference(expression: IrCallableReference<*>, data: D): R =
            visitMemberAccess(expression, data)

    open fun visitFunctionReference(expression: IrFunctionReference, data: D): R =
            visitCallableReference(expression, data)

    open fun visitPropertyReference(expression: IrPropertyReference, data: D): R =
            visitCallableReference(expression, data)

    open
            fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference,
            data: D): R = visitCallableReference(expression, data)

    open fun visitClassReference(expression: IrClassReference, data: D): R =
            visitDeclarationReference(expression, data)

    open fun visitConst(expression: IrConst<*>, data: D): R = visitExpression(expression,
            data)

    open fun visitConstantValue(expression: IrConstantValue, data: D): R =
            visitExpression(expression, data)

    open fun visitConstantPrimitive(expression: IrConstantPrimitive, data: D): R =
            visitConstantValue(expression, data)

    open fun visitConstantObject(expression: IrConstantObject, data: D): R =
            visitConstantValue(expression, data)

    open fun visitConstantArray(expression: IrConstantArray, data: D): R =
            visitConstantValue(expression, data)

    open fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall,
            data: D): R = visitFunctionAccess(expression, data)

    open fun visitDynamicExpression(expression: IrDynamicExpression, data: D): R =
            visitExpression(expression, data)

    open fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression,
            data: D): R = visitDynamicExpression(expression, data)

    open fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: D):
            R = visitDynamicExpression(expression, data)

    open fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: D): R =
            visitFunctionAccess(expression, data)

    open fun visitErrorExpression(expression: IrErrorExpression, data: D): R =
            visitExpression(expression, data)

    open fun visitErrorCallExpression(expression: IrErrorCallExpression, data: D): R =
            visitErrorExpression(expression, data)

    open fun visitFieldAccess(expression: IrFieldAccessExpression, data: D): R =
            visitDeclarationReference(expression, data)

    open fun visitGetField(expression: IrGetField, data: D): R =
            visitFieldAccess(expression, data)

    open fun visitSetField(expression: IrSetField, data: D): R =
            visitFieldAccess(expression, data)

    open fun visitFunctionExpression(expression: IrFunctionExpression, data: D): R =
            visitExpression(expression, data)

    open fun visitGetClass(expression: IrGetClass, data: D): R =
            visitExpression(expression, data)

    open fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: D):
            R = visitExpression(expression, data)

    open fun visitLoop(loop: IrLoop, data: D): R = visitExpression(loop, data)

    open fun visitWhileLoop(loop: IrWhileLoop, data: D): R = visitLoop(loop, data)

    open fun visitDoWhileLoop(loop: IrDoWhileLoop, data: D): R = visitLoop(loop, data)

    open fun visitReturn(expression: IrReturn, data: D): R = visitExpression(expression,
            data)

    open fun visitStringConcatenation(expression: IrStringConcatenation, data: D): R =
            visitExpression(expression, data)

    open fun visitSuspensionPoint(expression: IrSuspensionPoint, data: D): R =
            visitExpression(expression, data)

    open fun visitSuspendableExpression(expression: IrSuspendableExpression, data: D): R =
            visitExpression(expression, data)

    open fun visitThrow(expression: IrThrow, data: D): R = visitExpression(expression,
            data)

    open fun visitTry(aTry: IrTry, data: D): R = visitExpression(aTry, data)

    open fun visitCatch(aCatch: IrCatch, data: D): R = visitElement(aCatch, data)

    open fun visitTypeOperator(expression: IrTypeOperatorCall, data: D): R =
            visitExpression(expression, data)

    open fun visitValueAccess(expression: IrValueAccessExpression, data: D): R =
            visitDeclarationReference(expression, data)

    open fun visitGetValue(expression: IrGetValue, data: D): R =
            visitValueAccess(expression, data)

    open fun visitSetValue(expression: IrSetValue, data: D): R =
            visitValueAccess(expression, data)

    open fun visitVararg(expression: IrVararg, data: D): R = visitExpression(expression,
            data)

    open fun visitSpreadElement(spread: IrSpreadElement, data: D): R = visitElement(spread,
            data)

    open fun visitWhen(expression: IrWhen, data: D): R = visitExpression(expression, data)

    open fun visitBranch(branch: IrBranch, data: D): R = visitElement(branch, data)

    open fun visitElseBranch(branch: IrElseBranch, data: D): R = visitBranch(branch, data)
}
