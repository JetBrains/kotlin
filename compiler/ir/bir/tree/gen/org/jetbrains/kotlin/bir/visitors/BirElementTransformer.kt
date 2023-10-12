/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.visitors

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirStatement
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

interface BirElementTransformer<in D> : BirElementVisitor<BirElement, D> {
    override fun visitElement(element: BirElement, data: D): BirElement {
        element.transformChildren(this, data)
        return element
    }

    override fun visitDeclaration(declaration: BirDeclarationBase, data: D): BirStatement {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitValueParameter(declaration: BirValueParameter, data: D): BirStatement
            = visitDeclaration(declaration, data)

    override fun visitClass(declaration: BirClass, data: D): BirStatement =
            visitDeclaration(declaration, data)

    override fun visitAnonymousInitializer(declaration: BirAnonymousInitializer, data: D):
            BirStatement = visitDeclaration(declaration, data)

    override fun visitTypeParameter(declaration: BirTypeParameter, data: D): BirStatement =
            visitDeclaration(declaration, data)

    override fun visitFunction(declaration: BirFunction, data: D): BirStatement =
            visitDeclaration(declaration, data)

    override fun visitConstructor(declaration: BirConstructor, data: D): BirStatement =
            visitFunction(declaration, data)

    override fun visitEnumEntry(declaration: BirEnumEntry, data: D): BirStatement =
            visitDeclaration(declaration, data)

    override fun visitErrorDeclaration(declaration: BirErrorDeclaration, data: D):
            BirStatement = visitDeclaration(declaration, data)

    override fun visitField(declaration: BirField, data: D): BirStatement =
            visitDeclaration(declaration, data)

    override fun visitLocalDelegatedProperty(declaration: BirLocalDelegatedProperty,
            data: D): BirStatement = visitDeclaration(declaration, data)

    override fun visitModuleFragment(declaration: BirModuleFragment, data: D):
            BirModuleFragment {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitProperty(declaration: BirProperty, data: D): BirStatement =
            visitDeclaration(declaration, data)

    override fun visitScript(declaration: BirScript, data: D): BirStatement =
            visitDeclaration(declaration, data)

    override fun visitSimpleFunction(declaration: BirSimpleFunction, data: D): BirStatement
            = visitFunction(declaration, data)

    override fun visitTypeAlias(declaration: BirTypeAlias, data: D): BirStatement =
            visitDeclaration(declaration, data)

    override fun visitVariable(declaration: BirVariable, data: D): BirStatement =
            visitDeclaration(declaration, data)

    override fun visitPackageFragment(declaration: BirPackageFragment, data: D): BirElement
            = visitElement(declaration, data)

    override fun visitExternalPackageFragment(declaration: BirExternalPackageFragment,
            data: D): BirExternalPackageFragment {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitFile(declaration: BirFile, data: D): BirFile {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun visitExpression(expression: BirExpression, data: D): BirExpression {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitBody(body: BirBody, data: D): BirBody {
        body.transformChildren(this, data)
        return body
    }

    override fun visitExpressionBody(body: BirExpressionBody, data: D): BirBody =
            visitBody(body, data)

    override fun visitBlockBody(body: BirBlockBody, data: D): BirBody = visitBody(body,
            data)

    override fun visitDeclarationReference(expression: BirDeclarationReference, data: D):
            BirExpression = visitExpression(expression, data)

    override fun visitMemberAccess(expression: BirMemberAccessExpression<*>, data: D):
            BirElement = visitDeclarationReference(expression, data)

    override fun visitFunctionAccess(expression: BirFunctionAccessExpression, data: D):
            BirElement = visitMemberAccess(expression, data)

    override fun visitConstructorCall(expression: BirConstructorCall, data: D): BirElement
            = visitFunctionAccess(expression, data)

    override fun visitSingletonReference(expression: BirGetSingletonValue, data: D):
            BirExpression = visitDeclarationReference(expression, data)

    override fun visitGetObjectValue(expression: BirGetObjectValue, data: D): BirExpression
            = visitSingletonReference(expression, data)

    override fun visitGetEnumValue(expression: BirGetEnumValue, data: D): BirExpression =
            visitSingletonReference(expression, data)

    override fun visitRawFunctionReference(expression: BirRawFunctionReference, data: D):
            BirExpression = visitDeclarationReference(expression, data)

    override fun visitContainerExpression(expression: BirContainerExpression, data: D):
            BirExpression = visitExpression(expression, data)

    override fun visitBlock(expression: BirBlock, data: D): BirExpression =
            visitContainerExpression(expression, data)

    override fun visitComposite(expression: BirComposite, data: D): BirExpression =
            visitContainerExpression(expression, data)

    override fun visitSyntheticBody(body: BirSyntheticBody, data: D): BirBody =
            visitBody(body, data)

    override fun visitBreakContinue(jump: BirBreakContinue, data: D): BirExpression =
            visitExpression(jump, data)

    override fun visitBreak(jump: BirBreak, data: D): BirExpression =
            visitBreakContinue(jump, data)

    override fun visitContinue(jump: BirContinue, data: D): BirExpression =
            visitBreakContinue(jump, data)

    override fun visitCall(expression: BirCall, data: D): BirElement =
            visitFunctionAccess(expression, data)

    override fun visitCallableReference(expression: BirCallableReference<*>, data: D):
            BirElement = visitMemberAccess(expression, data)

    override fun visitFunctionReference(expression: BirFunctionReference, data: D):
            BirElement = visitCallableReference(expression, data)

    override fun visitPropertyReference(expression: BirPropertyReference, data: D):
            BirElement = visitCallableReference(expression, data)

    override
            fun visitLocalDelegatedPropertyReference(expression: BirLocalDelegatedPropertyReference,
            data: D): BirElement = visitCallableReference(expression, data)

    override fun visitClassReference(expression: BirClassReference, data: D): BirExpression
            = visitDeclarationReference(expression, data)

    override fun visitConst(expression: BirConst<*>, data: D): BirExpression =
            visitExpression(expression, data)

    override fun visitConstantValue(expression: BirConstantValue, data: D):
            BirConstantValue {
        expression.transformChildren(this, data)
        return expression
    }

    override fun visitConstantPrimitive(expression: BirConstantPrimitive, data: D):
            BirConstantValue = visitConstantValue(expression, data)

    override fun visitConstantObject(expression: BirConstantObject, data: D):
            BirConstantValue = visitConstantValue(expression, data)

    override fun visitConstantArray(expression: BirConstantArray, data: D):
            BirConstantValue = visitConstantValue(expression, data)

    override fun visitDelegatingConstructorCall(expression: BirDelegatingConstructorCall,
            data: D): BirElement = visitFunctionAccess(expression, data)

    override fun visitDynamicExpression(expression: BirDynamicExpression, data: D):
            BirExpression = visitExpression(expression, data)

    override fun visitDynamicOperatorExpression(expression: BirDynamicOperatorExpression,
            data: D): BirExpression = visitDynamicExpression(expression, data)

    override fun visitDynamicMemberExpression(expression: BirDynamicMemberExpression,
            data: D): BirExpression = visitDynamicExpression(expression, data)

    override fun visitEnumConstructorCall(expression: BirEnumConstructorCall, data: D):
            BirElement = visitFunctionAccess(expression, data)

    override fun visitErrorExpression(expression: BirErrorExpression, data: D):
            BirExpression = visitExpression(expression, data)

    override fun visitErrorCallExpression(expression: BirErrorCallExpression, data: D):
            BirExpression = visitErrorExpression(expression, data)

    override fun visitFieldAccess(expression: BirFieldAccessExpression, data: D):
            BirExpression = visitDeclarationReference(expression, data)

    override fun visitGetField(expression: BirGetField, data: D): BirExpression =
            visitFieldAccess(expression, data)

    override fun visitSetField(expression: BirSetField, data: D): BirExpression =
            visitFieldAccess(expression, data)

    override fun visitFunctionExpression(expression: BirFunctionExpression, data: D):
            BirElement = visitExpression(expression, data)

    override fun visitGetClass(expression: BirGetClass, data: D): BirExpression =
            visitExpression(expression, data)

    override fun visitInstanceInitializerCall(expression: BirInstanceInitializerCall,
            data: D): BirExpression = visitExpression(expression, data)

    override fun visitLoop(loop: BirLoop, data: D): BirExpression = visitExpression(loop,
            data)

    override fun visitWhileLoop(loop: BirWhileLoop, data: D): BirExpression =
            visitLoop(loop, data)

    override fun visitDoWhileLoop(loop: BirDoWhileLoop, data: D): BirExpression =
            visitLoop(loop, data)

    override fun visitReturn(expression: BirReturn, data: D): BirExpression =
            visitExpression(expression, data)

    override fun visitStringConcatenation(expression: BirStringConcatenation, data: D):
            BirExpression = visitExpression(expression, data)

    override fun visitSuspensionPoint(expression: BirSuspensionPoint, data: D):
            BirExpression = visitExpression(expression, data)

    override fun visitSuspendableExpression(expression: BirSuspendableExpression, data: D):
            BirExpression = visitExpression(expression, data)

    override fun visitThrow(expression: BirThrow, data: D): BirExpression =
            visitExpression(expression, data)

    override fun visitTry(aTry: BirTry, data: D): BirExpression = visitExpression(aTry,
            data)

    override fun visitCatch(aCatch: BirCatch, data: D): BirCatch {
        aCatch.transformChildren(this, data)
        return aCatch
    }

    override fun visitTypeOperator(expression: BirTypeOperatorCall, data: D): BirExpression
            = visitExpression(expression, data)

    override fun visitValueAccess(expression: BirValueAccessExpression, data: D):
            BirExpression = visitDeclarationReference(expression, data)

    override fun visitGetValue(expression: BirGetValue, data: D): BirExpression =
            visitValueAccess(expression, data)

    override fun visitSetValue(expression: BirSetValue, data: D): BirExpression =
            visitValueAccess(expression, data)

    override fun visitVararg(expression: BirVararg, data: D): BirExpression =
            visitExpression(expression, data)

    override fun visitSpreadElement(spread: BirSpreadElement, data: D): BirSpreadElement {
        spread.transformChildren(this, data)
        return spread
    }

    override fun visitWhen(expression: BirWhen, data: D): BirExpression =
            visitExpression(expression, data)

    override fun visitBranch(branch: BirBranch, data: D): BirBranch {
        branch.transformChildren(this, data)
        return branch
    }

    override fun visitElseBranch(branch: BirElseBranch, data: D): BirElseBranch {
        branch.transformChildren(this, data)
        return branch
    }
}
