/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.visitors

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.declarations.BirAnonymousInitializer
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirConstructor
import org.jetbrains.kotlin.bir.declarations.BirDeclarationBase
import org.jetbrains.kotlin.bir.declarations.BirEnumEntry
import org.jetbrains.kotlin.bir.declarations.BirErrorDeclaration
import org.jetbrains.kotlin.bir.declarations.BirExternalPackageFragment
import org.jetbrains.kotlin.bir.declarations.BirField
import org.jetbrains.kotlin.bir.declarations.BirFile
import org.jetbrains.kotlin.bir.declarations.BirFunction
import org.jetbrains.kotlin.bir.declarations.BirLocalDelegatedProperty
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.declarations.BirPackageFragment
import org.jetbrains.kotlin.bir.declarations.BirProperty
import org.jetbrains.kotlin.bir.declarations.BirScript
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.declarations.BirTypeAlias
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.declarations.BirVariable
import org.jetbrains.kotlin.bir.expressions.BirBlock
import org.jetbrains.kotlin.bir.expressions.BirBlockBody
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirBranch
import org.jetbrains.kotlin.bir.expressions.BirBreak
import org.jetbrains.kotlin.bir.expressions.BirBreakContinue
import org.jetbrains.kotlin.bir.expressions.BirCall
import org.jetbrains.kotlin.bir.expressions.BirCallableReference
import org.jetbrains.kotlin.bir.expressions.BirCatch
import org.jetbrains.kotlin.bir.expressions.BirClassReference
import org.jetbrains.kotlin.bir.expressions.BirComposite
import org.jetbrains.kotlin.bir.expressions.BirConst
import org.jetbrains.kotlin.bir.expressions.BirConstantArray
import org.jetbrains.kotlin.bir.expressions.BirConstantObject
import org.jetbrains.kotlin.bir.expressions.BirConstantPrimitive
import org.jetbrains.kotlin.bir.expressions.BirConstantValue
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirContainerExpression
import org.jetbrains.kotlin.bir.expressions.BirContinue
import org.jetbrains.kotlin.bir.expressions.BirDeclarationReference
import org.jetbrains.kotlin.bir.expressions.BirDelegatingConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirDoWhileLoop
import org.jetbrains.kotlin.bir.expressions.BirDynamicExpression
import org.jetbrains.kotlin.bir.expressions.BirDynamicMemberExpression
import org.jetbrains.kotlin.bir.expressions.BirDynamicOperatorExpression
import org.jetbrains.kotlin.bir.expressions.BirElseBranch
import org.jetbrains.kotlin.bir.expressions.BirEnumConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirErrorCallExpression
import org.jetbrains.kotlin.bir.expressions.BirErrorExpression
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.expressions.BirFieldAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirFunctionAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirFunctionExpression
import org.jetbrains.kotlin.bir.expressions.BirFunctionReference
import org.jetbrains.kotlin.bir.expressions.BirGetClass
import org.jetbrains.kotlin.bir.expressions.BirGetEnumValue
import org.jetbrains.kotlin.bir.expressions.BirGetField
import org.jetbrains.kotlin.bir.expressions.BirGetObjectValue
import org.jetbrains.kotlin.bir.expressions.BirGetSingletonValue
import org.jetbrains.kotlin.bir.expressions.BirGetValue
import org.jetbrains.kotlin.bir.expressions.BirInstanceInitializerCall
import org.jetbrains.kotlin.bir.expressions.BirLocalDelegatedPropertyReference
import org.jetbrains.kotlin.bir.expressions.BirLoop
import org.jetbrains.kotlin.bir.expressions.BirMemberAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirPropertyReference
import org.jetbrains.kotlin.bir.expressions.BirRawFunctionReference
import org.jetbrains.kotlin.bir.expressions.BirReturn
import org.jetbrains.kotlin.bir.expressions.BirSetField
import org.jetbrains.kotlin.bir.expressions.BirSetValue
import org.jetbrains.kotlin.bir.expressions.BirSpreadElement
import org.jetbrains.kotlin.bir.expressions.BirStringConcatenation
import org.jetbrains.kotlin.bir.expressions.BirSuspendableExpression
import org.jetbrains.kotlin.bir.expressions.BirSuspensionPoint
import org.jetbrains.kotlin.bir.expressions.BirSyntheticBody
import org.jetbrains.kotlin.bir.expressions.BirThrow
import org.jetbrains.kotlin.bir.expressions.BirTry
import org.jetbrains.kotlin.bir.expressions.BirTypeOperatorCall
import org.jetbrains.kotlin.bir.expressions.BirValueAccessExpression
import org.jetbrains.kotlin.bir.expressions.BirVararg
import org.jetbrains.kotlin.bir.expressions.BirWhen
import org.jetbrains.kotlin.bir.expressions.BirWhileLoop

interface BirElementVisitor<out R, in D> {
    fun visitElement(element: BirElement, data: D): R

    fun visitDeclaration(declaration: BirDeclarationBase, data: D): R =
            visitElement(declaration, data)

    fun visitValueParameter(declaration: BirValueParameter, data: D): R =
            visitDeclaration(declaration, data)

    fun visitClass(declaration: BirClass, data: D): R = visitDeclaration(declaration, data)

    fun visitAnonymousInitializer(declaration: BirAnonymousInitializer, data: D): R =
            visitDeclaration(declaration, data)

    fun visitTypeParameter(declaration: BirTypeParameter, data: D): R =
            visitDeclaration(declaration, data)

    fun visitFunction(declaration: BirFunction, data: D): R = visitDeclaration(declaration,
            data)

    fun visitConstructor(declaration: BirConstructor, data: D): R =
            visitFunction(declaration, data)

    fun visitEnumEntry(declaration: BirEnumEntry, data: D): R =
            visitDeclaration(declaration, data)

    fun visitErrorDeclaration(declaration: BirErrorDeclaration, data: D): R =
            visitDeclaration(declaration, data)

    fun visitField(declaration: BirField, data: D): R = visitDeclaration(declaration, data)

    fun visitLocalDelegatedProperty(declaration: BirLocalDelegatedProperty, data: D): R =
            visitDeclaration(declaration, data)

    fun visitModuleFragment(declaration: BirModuleFragment, data: D): R =
            visitElement(declaration, data)

    fun visitProperty(declaration: BirProperty, data: D): R = visitDeclaration(declaration,
            data)

    fun visitScript(declaration: BirScript, data: D): R = visitDeclaration(declaration,
            data)

    fun visitSimpleFunction(declaration: BirSimpleFunction, data: D): R =
            visitFunction(declaration, data)

    fun visitTypeAlias(declaration: BirTypeAlias, data: D): R =
            visitDeclaration(declaration, data)

    fun visitVariable(declaration: BirVariable, data: D): R = visitDeclaration(declaration,
            data)

    fun visitPackageFragment(declaration: BirPackageFragment, data: D): R =
            visitElement(declaration, data)

    fun visitExternalPackageFragment(declaration: BirExternalPackageFragment, data: D): R =
            visitPackageFragment(declaration, data)

    fun visitFile(declaration: BirFile, data: D): R = visitPackageFragment(declaration,
            data)

    fun visitExpression(expression: BirExpression, data: D): R = visitElement(expression,
            data)

    fun visitBody(body: BirBody, data: D): R = visitElement(body, data)

    fun visitExpressionBody(body: BirExpressionBody, data: D): R = visitBody(body, data)

    fun visitBlockBody(body: BirBlockBody, data: D): R = visitBody(body, data)

    fun visitDeclarationReference(expression: BirDeclarationReference, data: D): R =
            visitExpression(expression, data)

    fun visitMemberAccess(expression: BirMemberAccessExpression<*>, data: D): R =
            visitDeclarationReference(expression, data)

    fun visitFunctionAccess(expression: BirFunctionAccessExpression, data: D): R =
            visitMemberAccess(expression, data)

    fun visitConstructorCall(expression: BirConstructorCall, data: D): R =
            visitFunctionAccess(expression, data)

    fun visitSingletonReference(expression: BirGetSingletonValue, data: D): R =
            visitDeclarationReference(expression, data)

    fun visitGetObjectValue(expression: BirGetObjectValue, data: D): R =
            visitSingletonReference(expression, data)

    fun visitGetEnumValue(expression: BirGetEnumValue, data: D): R =
            visitSingletonReference(expression, data)

    fun visitRawFunctionReference(expression: BirRawFunctionReference, data: D): R =
            visitDeclarationReference(expression, data)

    fun visitContainerExpression(expression: BirContainerExpression, data: D): R =
            visitExpression(expression, data)

    fun visitBlock(expression: BirBlock, data: D): R = visitContainerExpression(expression,
            data)

    fun visitComposite(expression: BirComposite, data: D): R =
            visitContainerExpression(expression, data)

    fun visitSyntheticBody(body: BirSyntheticBody, data: D): R = visitBody(body, data)

    fun visitBreakContinue(jump: BirBreakContinue, data: D): R = visitExpression(jump,
            data)

    fun visitBreak(jump: BirBreak, data: D): R = visitBreakContinue(jump, data)

    fun visitContinue(jump: BirContinue, data: D): R = visitBreakContinue(jump, data)

    fun visitCall(expression: BirCall, data: D): R = visitFunctionAccess(expression, data)

    fun visitCallableReference(expression: BirCallableReference<*>, data: D): R =
            visitMemberAccess(expression, data)

    fun visitFunctionReference(expression: BirFunctionReference, data: D): R =
            visitCallableReference(expression, data)

    fun visitPropertyReference(expression: BirPropertyReference, data: D): R =
            visitCallableReference(expression, data)

    fun visitLocalDelegatedPropertyReference(expression: BirLocalDelegatedPropertyReference,
            data: D): R = visitCallableReference(expression, data)

    fun visitClassReference(expression: BirClassReference, data: D): R =
            visitDeclarationReference(expression, data)

    fun visitConst(expression: BirConst<*>, data: D): R = visitExpression(expression, data)

    fun visitConstantValue(expression: BirConstantValue, data: D): R =
            visitExpression(expression, data)

    fun visitConstantPrimitive(expression: BirConstantPrimitive, data: D): R =
            visitConstantValue(expression, data)

    fun visitConstantObject(expression: BirConstantObject, data: D): R =
            visitConstantValue(expression, data)

    fun visitConstantArray(expression: BirConstantArray, data: D): R =
            visitConstantValue(expression, data)

    fun visitDelegatingConstructorCall(expression: BirDelegatingConstructorCall, data: D):
            R = visitFunctionAccess(expression, data)

    fun visitDynamicExpression(expression: BirDynamicExpression, data: D): R =
            visitExpression(expression, data)

    fun visitDynamicOperatorExpression(expression: BirDynamicOperatorExpression, data: D):
            R = visitDynamicExpression(expression, data)

    fun visitDynamicMemberExpression(expression: BirDynamicMemberExpression, data: D): R =
            visitDynamicExpression(expression, data)

    fun visitEnumConstructorCall(expression: BirEnumConstructorCall, data: D): R =
            visitFunctionAccess(expression, data)

    fun visitErrorExpression(expression: BirErrorExpression, data: D): R =
            visitExpression(expression, data)

    fun visitErrorCallExpression(expression: BirErrorCallExpression, data: D): R =
            visitErrorExpression(expression, data)

    fun visitFieldAccess(expression: BirFieldAccessExpression, data: D): R =
            visitDeclarationReference(expression, data)

    fun visitGetField(expression: BirGetField, data: D): R = visitFieldAccess(expression,
            data)

    fun visitSetField(expression: BirSetField, data: D): R = visitFieldAccess(expression,
            data)

    fun visitFunctionExpression(expression: BirFunctionExpression, data: D): R =
            visitExpression(expression, data)

    fun visitGetClass(expression: BirGetClass, data: D): R = visitExpression(expression,
            data)

    fun visitInstanceInitializerCall(expression: BirInstanceInitializerCall, data: D): R =
            visitExpression(expression, data)

    fun visitLoop(loop: BirLoop, data: D): R = visitExpression(loop, data)

    fun visitWhileLoop(loop: BirWhileLoop, data: D): R = visitLoop(loop, data)

    fun visitDoWhileLoop(loop: BirDoWhileLoop, data: D): R = visitLoop(loop, data)

    fun visitReturn(expression: BirReturn, data: D): R = visitExpression(expression, data)

    fun visitStringConcatenation(expression: BirStringConcatenation, data: D): R =
            visitExpression(expression, data)

    fun visitSuspensionPoint(expression: BirSuspensionPoint, data: D): R =
            visitExpression(expression, data)

    fun visitSuspendableExpression(expression: BirSuspendableExpression, data: D): R =
            visitExpression(expression, data)

    fun visitThrow(expression: BirThrow, data: D): R = visitExpression(expression, data)

    fun visitTry(aTry: BirTry, data: D): R = visitExpression(aTry, data)

    fun visitCatch(aCatch: BirCatch, data: D): R = visitElement(aCatch, data)

    fun visitTypeOperator(expression: BirTypeOperatorCall, data: D): R =
            visitExpression(expression, data)

    fun visitValueAccess(expression: BirValueAccessExpression, data: D): R =
            visitDeclarationReference(expression, data)

    fun visitGetValue(expression: BirGetValue, data: D): R = visitValueAccess(expression,
            data)

    fun visitSetValue(expression: BirSetValue, data: D): R = visitValueAccess(expression,
            data)

    fun visitVararg(expression: BirVararg, data: D): R = visitExpression(expression, data)

    fun visitSpreadElement(spread: BirSpreadElement, data: D): R = visitElement(spread,
            data)

    fun visitWhen(expression: BirWhen, data: D): R = visitExpression(expression, data)

    fun visitBranch(branch: BirBranch, data: D): R = visitElement(branch, data)

    fun visitElseBranch(branch: BirElseBranch, data: D): R = visitBranch(branch, data)
}
