    /*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

class IrImportResolverVisitor(val resolvedImports: HashSet<String>) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        TODO("not implemented")
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        declaration.body.accept(this, null)
    }

    override fun visitBlock(expression: IrBlock) {
        with(expression){
            resolvedImports.add(type.classifierOrNull?.renderClassifierFqn())
            statements.forEach {it.accept(this@IrImportResolverVisitor, null)}

        }
    }

    override fun visitBlockBody(body: IrBlockBody) {
        TODO("not implemented")
    }

    override fun visitBranch(branch: IrBranch) {
        TODO("not implemented")
    }

    override fun visitBreak(jump: IrBreak) {
        TODO("not implemented")
    }

    override fun visitCall(expression: IrCall) {
        TODO("not implemented")
    }

    override fun visitCallableReference(expression: IrCallableReference) {
        TODO("not implemented")
    }

    override fun visitClass(declaration: IrClass) {
        TODO("not implemented")
    }

    override fun visitCatch(aCatch: IrCatch) {
        TODO("not implemented")
    }

    override fun visitClassReference(expression: IrClassReference) {
        TODO("not implemented")
    }

    override fun visitComposite(expression: IrComposite) {
        TODO("not implemented")
    }

    override fun <T> visitConst(expression: IrConst<T>) {
        TODO("not implemented")
    }

    override fun visitConstructor(declaration: IrConstructor) {
        TODO("not implemented")
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        TODO("not implemented")
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        TODO("not implemented")
    }

    override fun visitContinue(jump: IrContinue) {
        TODO("not implemented")
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        TODO("not implemented")
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        TODO("not implemented")
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        TODO("not implemented")
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        TODO("not implemented")
    }

    override fun visitDynamicExpression(expression: IrDynamicExpression) {
        TODO("not implemented")
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression) {
        TODO("not implemented")
    }

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression) {
        TODO("not implemented")
    }

    override fun visitElseBranch(branch: IrElseBranch) {
        TODO("not implemented")
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) {
        TODO("not implemented")
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        TODO("not implemented")
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression) {
        TODO("not implemented")
    }

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration) {
        TODO("not implemented")
    }

    override fun visitErrorExpression(expression: IrErrorExpression) {
        TODO("not implemented")
    }

    override fun visitExpression(expression: IrExpression) {
        TODO("not implemented")
    }

    override fun visitExpressionBody(body: IrExpressionBody) {
        TODO("not implemented")
    }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) {
        TODO("not implemented")
    }

    override fun visitField(declaration: IrField) {
        TODO("not implemented")
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        TODO("not implemented")
    }

    override fun visitFile(declaration: IrFile) {
        TODO("not implemented")
    }

    override fun visitFunction(declaration: IrFunction) {
        TODO("not implemented")
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        TODO("not implemented")
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression) {
        TODO("not implemented")
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        TODO("not implemented")
    }

    override fun visitGetClass(expression: IrGetClass) {
        TODO("not implemented")
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue) {
        TODO("not implemented")
    }

    override fun visitGetField(expression: IrGetField) {
        TODO("not implemented")
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue) {
        TODO("not implemented")
    }

    override fun visitGetValue(expression: IrGetValue) {
        TODO("not implemented")
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        TODO("not implemented")
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        TODO("not implemented")
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        TODO("not implemented")
    }

    override fun visitLoop(loop: IrLoop) {
        TODO("not implemented")
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression) {
        TODO("not implemented")
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) {
        TODO("not implemented")
    }

    override fun visitPackageFragment(declaration: IrPackageFragment) {
        TODO("not implemented")
    }

    override fun visitProperty(declaration: IrProperty) {
        TODO("not implemented")
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        TODO("not implemented")
    }

    override fun visitReturn(expression: IrReturn) {
        TODO("not implemented")
    }

    override fun visitSetField(expression: IrSetField) {
        TODO("not implemented")
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        TODO("not implemented")
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        TODO("not implemented")
    }

    override fun visitSingletonReference(expression: IrGetSingletonValue) {
        TODO("not implemented")
    }

    override fun visitSpreadElement(spread: IrSpreadElement) {
        TODO("not implemented")
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        TODO("not implemented")
    }

    override fun visitSuspendableExpression(expression: IrSuspendableExpression) {
        TODO("not implemented")
    }

    override fun visitSuspensionPoint(expression: IrSuspensionPoint) {
        TODO("not implemented")
    }

    override fun visitSyntheticBody(body: IrSyntheticBody) {
        TODO("not implemented")
    }

    override fun visitThrow(expression: IrThrow) {
        TODO("not implemented")
    }

    override fun visitTry(aTry: IrTry) {
        TODO("not implemented")
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        TODO("not implemented")
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        TODO("not implemented")
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        TODO("not implemented")
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        TODO("not implemented")
    }

    override fun visitVararg(expression: IrVararg) {
        TODO("not implemented")
    }

    override fun visitVariable(declaration: IrVariable) {
        TODO("not implemented")
    }

    override fun visitWhen(expression: IrWhen) {
        TODO("not implemented")
    }

    override fun visitWhileLoop(loop: IrWhileLoop) {
        TODO("not implemented")
    }
}