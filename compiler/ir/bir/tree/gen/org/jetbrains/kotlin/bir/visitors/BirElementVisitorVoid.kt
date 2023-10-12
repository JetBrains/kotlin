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

interface BirElementVisitorVoid : BirElementVisitor<Unit, Nothing?> {
    override fun visitElement(element: BirElement, data: Nothing?) = visitElement(element)

    fun visitElement(element: BirElement) {
    }

    override fun visitDeclaration(declaration: BirDeclarationBase, data: Nothing?) =
            visitDeclaration(declaration)

    fun visitDeclaration(declaration: BirDeclarationBase) = visitElement(declaration)

    override fun visitValueParameter(declaration: BirValueParameter, data: Nothing?) =
            visitValueParameter(declaration)

    fun visitValueParameter(declaration: BirValueParameter) = visitDeclaration(declaration)

    override fun visitClass(declaration: BirClass, data: Nothing?) =
            visitClass(declaration)

    fun visitClass(declaration: BirClass) = visitDeclaration(declaration)

    override fun visitAnonymousInitializer(declaration: BirAnonymousInitializer,
            data: Nothing?) = visitAnonymousInitializer(declaration)

    fun visitAnonymousInitializer(declaration: BirAnonymousInitializer) =
            visitDeclaration(declaration)

    override fun visitTypeParameter(declaration: BirTypeParameter, data: Nothing?) =
            visitTypeParameter(declaration)

    fun visitTypeParameter(declaration: BirTypeParameter) = visitDeclaration(declaration)

    override fun visitFunction(declaration: BirFunction, data: Nothing?) =
            visitFunction(declaration)

    fun visitFunction(declaration: BirFunction) = visitDeclaration(declaration)

    override fun visitConstructor(declaration: BirConstructor, data: Nothing?) =
            visitConstructor(declaration)

    fun visitConstructor(declaration: BirConstructor) = visitFunction(declaration)

    override fun visitEnumEntry(declaration: BirEnumEntry, data: Nothing?) =
            visitEnumEntry(declaration)

    fun visitEnumEntry(declaration: BirEnumEntry) = visitDeclaration(declaration)

    override fun visitErrorDeclaration(declaration: BirErrorDeclaration, data: Nothing?) =
            visitErrorDeclaration(declaration)

    fun visitErrorDeclaration(declaration: BirErrorDeclaration) =
            visitDeclaration(declaration)

    override fun visitField(declaration: BirField, data: Nothing?) =
            visitField(declaration)

    fun visitField(declaration: BirField) = visitDeclaration(declaration)

    override fun visitLocalDelegatedProperty(declaration: BirLocalDelegatedProperty,
            data: Nothing?) = visitLocalDelegatedProperty(declaration)

    fun visitLocalDelegatedProperty(declaration: BirLocalDelegatedProperty) =
            visitDeclaration(declaration)

    override fun visitModuleFragment(declaration: BirModuleFragment, data: Nothing?) =
            visitModuleFragment(declaration)

    fun visitModuleFragment(declaration: BirModuleFragment) = visitElement(declaration)

    override fun visitProperty(declaration: BirProperty, data: Nothing?) =
            visitProperty(declaration)

    fun visitProperty(declaration: BirProperty) = visitDeclaration(declaration)

    override fun visitScript(declaration: BirScript, data: Nothing?) =
            visitScript(declaration)

    fun visitScript(declaration: BirScript) = visitDeclaration(declaration)

    override fun visitSimpleFunction(declaration: BirSimpleFunction, data: Nothing?) =
            visitSimpleFunction(declaration)

    fun visitSimpleFunction(declaration: BirSimpleFunction) = visitFunction(declaration)

    override fun visitTypeAlias(declaration: BirTypeAlias, data: Nothing?) =
            visitTypeAlias(declaration)

    fun visitTypeAlias(declaration: BirTypeAlias) = visitDeclaration(declaration)

    override fun visitVariable(declaration: BirVariable, data: Nothing?) =
            visitVariable(declaration)

    fun visitVariable(declaration: BirVariable) = visitDeclaration(declaration)

    override fun visitPackageFragment(declaration: BirPackageFragment, data: Nothing?) =
            visitPackageFragment(declaration)

    fun visitPackageFragment(declaration: BirPackageFragment) = visitElement(declaration)

    override fun visitExternalPackageFragment(declaration: BirExternalPackageFragment,
            data: Nothing?) = visitExternalPackageFragment(declaration)

    fun visitExternalPackageFragment(declaration: BirExternalPackageFragment) =
            visitPackageFragment(declaration)

    override fun visitFile(declaration: BirFile, data: Nothing?) = visitFile(declaration)

    fun visitFile(declaration: BirFile) = visitPackageFragment(declaration)

    override fun visitExpression(expression: BirExpression, data: Nothing?) =
            visitExpression(expression)

    fun visitExpression(expression: BirExpression) = visitElement(expression)

    override fun visitBody(body: BirBody, data: Nothing?) = visitBody(body)

    fun visitBody(body: BirBody) = visitElement(body)

    override fun visitExpressionBody(body: BirExpressionBody, data: Nothing?) =
            visitExpressionBody(body)

    fun visitExpressionBody(body: BirExpressionBody) = visitBody(body)

    override fun visitBlockBody(body: BirBlockBody, data: Nothing?) = visitBlockBody(body)

    fun visitBlockBody(body: BirBlockBody) = visitBody(body)

    override fun visitDeclarationReference(expression: BirDeclarationReference,
            data: Nothing?) = visitDeclarationReference(expression)

    fun visitDeclarationReference(expression: BirDeclarationReference) =
            visitExpression(expression)

    override fun visitMemberAccess(expression: BirMemberAccessExpression<*>,
            data: Nothing?) = visitMemberAccess(expression)

    fun visitMemberAccess(expression: BirMemberAccessExpression<*>) =
            visitDeclarationReference(expression)

    override fun visitFunctionAccess(expression: BirFunctionAccessExpression,
            data: Nothing?) = visitFunctionAccess(expression)

    fun visitFunctionAccess(expression: BirFunctionAccessExpression) =
            visitMemberAccess(expression)

    override fun visitConstructorCall(expression: BirConstructorCall, data: Nothing?) =
            visitConstructorCall(expression)

    fun visitConstructorCall(expression: BirConstructorCall) =
            visitFunctionAccess(expression)

    override fun visitSingletonReference(expression: BirGetSingletonValue, data: Nothing?)
            = visitSingletonReference(expression)

    fun visitSingletonReference(expression: BirGetSingletonValue) =
            visitDeclarationReference(expression)

    override fun visitGetObjectValue(expression: BirGetObjectValue, data: Nothing?) =
            visitGetObjectValue(expression)

    fun visitGetObjectValue(expression: BirGetObjectValue) =
            visitSingletonReference(expression)

    override fun visitGetEnumValue(expression: BirGetEnumValue, data: Nothing?) =
            visitGetEnumValue(expression)

    fun visitGetEnumValue(expression: BirGetEnumValue) = visitSingletonReference(expression)

    override fun visitRawFunctionReference(expression: BirRawFunctionReference,
            data: Nothing?) = visitRawFunctionReference(expression)

    fun visitRawFunctionReference(expression: BirRawFunctionReference) =
            visitDeclarationReference(expression)

    override fun visitContainerExpression(expression: BirContainerExpression,
            data: Nothing?) = visitContainerExpression(expression)

    fun visitContainerExpression(expression: BirContainerExpression) =
            visitExpression(expression)

    override fun visitBlock(expression: BirBlock, data: Nothing?) = visitBlock(expression)

    fun visitBlock(expression: BirBlock) = visitContainerExpression(expression)

    override fun visitComposite(expression: BirComposite, data: Nothing?) =
            visitComposite(expression)

    fun visitComposite(expression: BirComposite) = visitContainerExpression(expression)

    override fun visitSyntheticBody(body: BirSyntheticBody, data: Nothing?) =
            visitSyntheticBody(body)

    fun visitSyntheticBody(body: BirSyntheticBody) = visitBody(body)

    override fun visitBreakContinue(jump: BirBreakContinue, data: Nothing?) =
            visitBreakContinue(jump)

    fun visitBreakContinue(jump: BirBreakContinue) = visitExpression(jump)

    override fun visitBreak(jump: BirBreak, data: Nothing?) = visitBreak(jump)

    fun visitBreak(jump: BirBreak) = visitBreakContinue(jump)

    override fun visitContinue(jump: BirContinue, data: Nothing?) = visitContinue(jump)

    fun visitContinue(jump: BirContinue) = visitBreakContinue(jump)

    override fun visitCall(expression: BirCall, data: Nothing?) = visitCall(expression)

    fun visitCall(expression: BirCall) = visitFunctionAccess(expression)

    override fun visitCallableReference(expression: BirCallableReference<*>,
            data: Nothing?) = visitCallableReference(expression)

    fun visitCallableReference(expression: BirCallableReference<*>) =
            visitMemberAccess(expression)

    override fun visitFunctionReference(expression: BirFunctionReference, data: Nothing?) =
            visitFunctionReference(expression)

    fun visitFunctionReference(expression: BirFunctionReference) =
            visitCallableReference(expression)

    override fun visitPropertyReference(expression: BirPropertyReference, data: Nothing?) =
            visitPropertyReference(expression)

    fun visitPropertyReference(expression: BirPropertyReference) =
            visitCallableReference(expression)

    override
            fun visitLocalDelegatedPropertyReference(expression: BirLocalDelegatedPropertyReference,
            data: Nothing?) = visitLocalDelegatedPropertyReference(expression)

    fun visitLocalDelegatedPropertyReference(expression: BirLocalDelegatedPropertyReference)
            = visitCallableReference(expression)

    override fun visitClassReference(expression: BirClassReference, data: Nothing?) =
            visitClassReference(expression)

    fun visitClassReference(expression: BirClassReference) =
            visitDeclarationReference(expression)

    override fun visitConst(expression: BirConst<*>, data: Nothing?) =
            visitConst(expression)

    fun visitConst(expression: BirConst<*>) = visitExpression(expression)

    override fun visitConstantValue(expression: BirConstantValue, data: Nothing?) =
            visitConstantValue(expression)

    fun visitConstantValue(expression: BirConstantValue) = visitExpression(expression)

    override fun visitConstantPrimitive(expression: BirConstantPrimitive, data: Nothing?) =
            visitConstantPrimitive(expression)

    fun visitConstantPrimitive(expression: BirConstantPrimitive) =
            visitConstantValue(expression)

    override fun visitConstantObject(expression: BirConstantObject, data: Nothing?) =
            visitConstantObject(expression)

    fun visitConstantObject(expression: BirConstantObject) = visitConstantValue(expression)

    override fun visitConstantArray(expression: BirConstantArray, data: Nothing?) =
            visitConstantArray(expression)

    fun visitConstantArray(expression: BirConstantArray) = visitConstantValue(expression)

    override fun visitDelegatingConstructorCall(expression: BirDelegatingConstructorCall,
            data: Nothing?) = visitDelegatingConstructorCall(expression)

    fun visitDelegatingConstructorCall(expression: BirDelegatingConstructorCall) =
            visitFunctionAccess(expression)

    override fun visitDynamicExpression(expression: BirDynamicExpression, data: Nothing?) =
            visitDynamicExpression(expression)

    fun visitDynamicExpression(expression: BirDynamicExpression) =
            visitExpression(expression)

    override fun visitDynamicOperatorExpression(expression: BirDynamicOperatorExpression,
            data: Nothing?) = visitDynamicOperatorExpression(expression)

    fun visitDynamicOperatorExpression(expression: BirDynamicOperatorExpression) =
            visitDynamicExpression(expression)

    override fun visitDynamicMemberExpression(expression: BirDynamicMemberExpression,
            data: Nothing?) = visitDynamicMemberExpression(expression)

    fun visitDynamicMemberExpression(expression: BirDynamicMemberExpression) =
            visitDynamicExpression(expression)

    override fun visitEnumConstructorCall(expression: BirEnumConstructorCall,
            data: Nothing?) = visitEnumConstructorCall(expression)

    fun visitEnumConstructorCall(expression: BirEnumConstructorCall) =
            visitFunctionAccess(expression)

    override fun visitErrorExpression(expression: BirErrorExpression, data: Nothing?) =
            visitErrorExpression(expression)

    fun visitErrorExpression(expression: BirErrorExpression) = visitExpression(expression)

    override fun visitErrorCallExpression(expression: BirErrorCallExpression,
            data: Nothing?) = visitErrorCallExpression(expression)

    fun visitErrorCallExpression(expression: BirErrorCallExpression) =
            visitErrorExpression(expression)

    override fun visitFieldAccess(expression: BirFieldAccessExpression, data: Nothing?) =
            visitFieldAccess(expression)

    fun visitFieldAccess(expression: BirFieldAccessExpression) =
            visitDeclarationReference(expression)

    override fun visitGetField(expression: BirGetField, data: Nothing?) =
            visitGetField(expression)

    fun visitGetField(expression: BirGetField) = visitFieldAccess(expression)

    override fun visitSetField(expression: BirSetField, data: Nothing?) =
            visitSetField(expression)

    fun visitSetField(expression: BirSetField) = visitFieldAccess(expression)

    override fun visitFunctionExpression(expression: BirFunctionExpression, data: Nothing?)
            = visitFunctionExpression(expression)

    fun visitFunctionExpression(expression: BirFunctionExpression) =
            visitExpression(expression)

    override fun visitGetClass(expression: BirGetClass, data: Nothing?) =
            visitGetClass(expression)

    fun visitGetClass(expression: BirGetClass) = visitExpression(expression)

    override fun visitInstanceInitializerCall(expression: BirInstanceInitializerCall,
            data: Nothing?) = visitInstanceInitializerCall(expression)

    fun visitInstanceInitializerCall(expression: BirInstanceInitializerCall) =
            visitExpression(expression)

    override fun visitLoop(loop: BirLoop, data: Nothing?) = visitLoop(loop)

    fun visitLoop(loop: BirLoop) = visitExpression(loop)

    override fun visitWhileLoop(loop: BirWhileLoop, data: Nothing?) = visitWhileLoop(loop)

    fun visitWhileLoop(loop: BirWhileLoop) = visitLoop(loop)

    override fun visitDoWhileLoop(loop: BirDoWhileLoop, data: Nothing?) =
            visitDoWhileLoop(loop)

    fun visitDoWhileLoop(loop: BirDoWhileLoop) = visitLoop(loop)

    override fun visitReturn(expression: BirReturn, data: Nothing?) =
            visitReturn(expression)

    fun visitReturn(expression: BirReturn) = visitExpression(expression)

    override fun visitStringConcatenation(expression: BirStringConcatenation,
            data: Nothing?) = visitStringConcatenation(expression)

    fun visitStringConcatenation(expression: BirStringConcatenation) =
            visitExpression(expression)

    override fun visitSuspensionPoint(expression: BirSuspensionPoint, data: Nothing?) =
            visitSuspensionPoint(expression)

    fun visitSuspensionPoint(expression: BirSuspensionPoint) = visitExpression(expression)

    override fun visitSuspendableExpression(expression: BirSuspendableExpression,
            data: Nothing?) = visitSuspendableExpression(expression)

    fun visitSuspendableExpression(expression: BirSuspendableExpression) =
            visitExpression(expression)

    override fun visitThrow(expression: BirThrow, data: Nothing?) = visitThrow(expression)

    fun visitThrow(expression: BirThrow) = visitExpression(expression)

    override fun visitTry(aTry: BirTry, data: Nothing?) = visitTry(aTry)

    fun visitTry(aTry: BirTry) = visitExpression(aTry)

    override fun visitCatch(aCatch: BirCatch, data: Nothing?) = visitCatch(aCatch)

    fun visitCatch(aCatch: BirCatch) = visitElement(aCatch)

    override fun visitTypeOperator(expression: BirTypeOperatorCall, data: Nothing?) =
            visitTypeOperator(expression)

    fun visitTypeOperator(expression: BirTypeOperatorCall) = visitExpression(expression)

    override fun visitValueAccess(expression: BirValueAccessExpression, data: Nothing?) =
            visitValueAccess(expression)

    fun visitValueAccess(expression: BirValueAccessExpression) =
            visitDeclarationReference(expression)

    override fun visitGetValue(expression: BirGetValue, data: Nothing?) =
            visitGetValue(expression)

    fun visitGetValue(expression: BirGetValue) = visitValueAccess(expression)

    override fun visitSetValue(expression: BirSetValue, data: Nothing?) =
            visitSetValue(expression)

    fun visitSetValue(expression: BirSetValue) = visitValueAccess(expression)

    override fun visitVararg(expression: BirVararg, data: Nothing?) =
            visitVararg(expression)

    fun visitVararg(expression: BirVararg) = visitExpression(expression)

    override fun visitSpreadElement(spread: BirSpreadElement, data: Nothing?) =
            visitSpreadElement(spread)

    fun visitSpreadElement(spread: BirSpreadElement) = visitElement(spread)

    override fun visitWhen(expression: BirWhen, data: Nothing?) = visitWhen(expression)

    fun visitWhen(expression: BirWhen) = visitExpression(expression)

    override fun visitBranch(branch: BirBranch, data: Nothing?) = visitBranch(branch)

    fun visitBranch(branch: BirBranch) = visitElement(branch)

    override fun visitElseBranch(branch: BirElseBranch, data: Nothing?) =
            visitElseBranch(branch)

    fun visitElseBranch(branch: BirElseBranch) = visitBranch(branch)
}
