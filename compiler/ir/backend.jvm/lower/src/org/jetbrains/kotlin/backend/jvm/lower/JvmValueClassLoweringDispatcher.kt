/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.loops.forLoopsPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

val jvmValueClassPhase = makeIrFilePhase(
    ::JvmValueClassLoweringDispatcher,
    name = "Value Classes",
    description = "Lower value classes",
    // forLoopsPhase may produce UInt and ULong which are inline classes.
    // Standard library replacements are done on the not mangled names for UInt and ULong classes.
    // Collection stubs may require mangling by value class rules.
    // SAM wrappers may require mangling for fun interfaces with value class parameters
    prerequisite = setOf(forLoopsPhase, jvmBuiltInsPhase, collectionStubMethodLowering, singleAbstractMethodPhase),
)

internal class JvmValueClassLoweringDispatcher(
    private val context: JvmBackendContext,
    private val fileClassNewDeclarations: MutableMap<IrFile, MutableList<IrSimpleFunction>> = mutableMapOf(),
    override val scopeStack: MutableList<ScopeWithIr> = mutableListOf(),
    private val inlineClassLowering: JvmInlineClassLowering = JvmInlineClassLowering(context, fileClassNewDeclarations, scopeStack),
    private val multiFieldValueClassLowering: JvmMultiFieldValueClassLowering =
        JvmMultiFieldValueClassLowering(context, fileClassNewDeclarations, scopeStack),
) : IrElementTransformerVoidWithContext(), FileLoweringPass {


    override fun lower(irFile: IrFile) = withinScope(irFile) {
        irFile.transformChildrenVoid()
        JvmValueClassAbstractLowering.addDeclarations(context, fileClassNewDeclarations, irFile)
    }


    private fun IrElement.requiresAnyHandling() =
        requiresHandling(multiFieldValueClassLowering) || requiresHandling(inlineClassLowering)

    override fun visitClassNew(declaration: IrClass): IrClass = if (declaration.requiresAnyHandling()) {
        declaration
            .let(multiFieldValueClassLowering::visitClassNew)
            .let(inlineClassLowering::visitClassNew)
    } else {
        declaration.transformChildrenVoid()
        for (innerDeclaration in declaration.declarations) {
            multiFieldValueClassLowering.visitClassNewDeclarationsWhenParallel(innerDeclaration)
        }
        for (innerDeclaration in declaration.declarations) {
            inlineClassLowering.visitClassNewDeclarationsWhenParallel(innerDeclaration)
        }
        declaration
    }

    private fun transformExpressionConsequently(expression: IrExpression) = expression
        .transform(multiFieldValueClassLowering, null)
        .transform(inlineClassLowering, null)

    private fun transformStatementConsequently(statement: IrStatement) = statement
        .transformStatement(multiFieldValueClassLowering)
        .transformStatement(inlineClassLowering)

    private fun IrElement.requiresHandling(lowering: JvmValueClassAbstractLowering) =
        accept(object : IrElementVisitor<Boolean, Nothing?> {
            override fun visitElement(element: IrElement, data: Nothing?): Boolean = false
            override fun visitClass(declaration: IrClass, data: Nothing?): Boolean =
                lowering.needsToVisitClassNew(declaration) || super.visitClass(declaration, data)

            override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): Boolean =
                lowering.needsToVisitFunctionReference(expression) || super.visitFunctionReference(expression, data)

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Nothing?): Boolean =
                lowering.needsToVisitFunctionAccess(expression) || super.visitFunctionAccess(expression, data)

            override fun visitCall(expression: IrCall, data: Nothing?): Boolean =
                lowering.needsToVisitCall(expression) || super.visitCall(expression, data)

            override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): Boolean =
                lowering.needsToVisitStringConcatenation(expression) || super.visitStringConcatenation(expression, data)

            override fun visitGetField(expression: IrGetField, data: Nothing?): Boolean =
                lowering.needsToVisitGetField(expression) || super.visitGetField(expression, data)

            override fun visitSetField(expression: IrSetField, data: Nothing?): Boolean =
                lowering.needsToVisitSetField(expression) || super.visitSetField(expression, data)

            override fun visitGetValue(expression: IrGetValue, data: Nothing?): Boolean =
                lowering.needsToVisitGetValue(expression) || super.visitGetValue(expression, data)

            override fun visitSetValue(expression: IrSetValue, data: Nothing?): Boolean =
                lowering.needsToVisitSetValue(expression) || super.visitSetValue(expression, data)

            override fun visitVariable(declaration: IrVariable, data: Nothing?): Boolean =
                lowering.needsToVisitVariable(declaration) || super.visitVariable(declaration, data)

            override fun visitReturn(expression: IrReturn, data: Nothing?): Boolean =
                lowering.needsToVisitReturn(expression) || super.visitReturn(expression, data)
        }, null)

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression = if (expression.requiresAnyHandling()) {
        transformExpressionConsequently(expression)
    } else {
        super.visitFunctionReference(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression = if (expression.requiresAnyHandling()) {
        transformExpressionConsequently(expression)
    } else {
        super.visitFunctionAccess(expression)
    }

    override fun visitCall(expression: IrCall): IrExpression = if (expression.requiresAnyHandling()) {
        transformExpressionConsequently(expression)
    } else {
        super.visitCall(expression)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression = if (expression.requiresAnyHandling()) {
        transformExpressionConsequently(expression)
    } else {
        super.visitStringConcatenation(expression)
    }

    override fun visitGetField(expression: IrGetField): IrExpression = if (expression.requiresAnyHandling()) {
        transformExpressionConsequently(expression)
    } else {
        super.visitGetField(expression)
    }

    override fun visitSetField(expression: IrSetField): IrExpression = if (expression.requiresAnyHandling()) {
        transformExpressionConsequently(expression)
    } else {
        super.visitSetField(expression)
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression = if (expression.requiresAnyHandling()) {
        transformExpressionConsequently(expression)
    } else {
        super.visitGetValue(expression)
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression = if (expression.requiresAnyHandling()) {
        transformExpressionConsequently(expression)
    } else {
        super.visitSetValue(expression)
    }

    override fun visitVariable(declaration: IrVariable): IrStatement = if (declaration.requiresAnyHandling()) {
        transformStatementConsequently(declaration)
    } else {
        super.visitVariable(declaration)
    }

    override fun visitReturn(expression: IrReturn): IrExpression = if (expression.requiresAnyHandling()) {
        transformExpressionConsequently(expression)
    } else {
        super.visitReturn(expression)
    }

    override fun visitAnonymousInitializerNew(declaration: IrAnonymousInitializer): IrStatement = if (declaration.requiresAnyHandling()) {
        transformStatementConsequently(declaration)
    } else {
        super.visitAnonymousInitializerNew(declaration)
    }

    private fun visitStatementContainer(container: IrStatementContainer) = if (container.statements.any { it.requiresAnyHandling() }) {
        multiFieldValueClassLowering.visitStatementContainer(container)
        inlineClassLowering.visitStatementContainer(container)
    } else {
        container.statements.replaceAll { it.transformStatement(this) }
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        visitStatementContainer(expression)
        return expression
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        visitStatementContainer(body)
        return body
    }
}