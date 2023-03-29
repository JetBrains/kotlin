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
import org.jetbrains.kotlin.backend.jvm.MemoizedValueClassLoweringDispatcherSharedData.Access.Body
import org.jetbrains.kotlin.backend.jvm.MemoizedValueClassLoweringDispatcherSharedData.Access.Header
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

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

internal class JvmValueClassLoweringDispatcher(private val context: JvmBackendContext) : IrElementTransformerVoidWithContext(),
    FileLoweringPass {
    override val scopeStack: MutableList<ScopeWithIr> = mutableListOf()
    private val inlineClassLowering: JvmInlineClassLowering = JvmInlineClassLowering(context, scopeStack)
    private val multiFieldValueClassLowering: JvmMultiFieldValueClassLowering = JvmMultiFieldValueClassLowering(context, scopeStack)


    override fun lower(irFile: IrFile) = withinScope(irFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitClassNew(declaration: IrClass): IrClass = if (declaration.requiresHandling()) {
        declaration
            .let(multiFieldValueClassLowering::visitClassNew)
            .let(inlineClassLowering::visitClassNew)
    } else {
        declaration
    }

    private fun IrElement.requiresHandling(): Boolean {
        val visitor = NeedsToVisit(context)
        accept(visitor, null)
        return visitor.result
    }
}

private class NeedsToVisit(private val context: JvmBackendContext) : IrElementVisitorVoid {
    var result = false
    private val replacements = context.valueClassLoweringDispatcherSharedData
    private val visitedParameters = mutableSetOf<IrSymbol>()

    override fun visitElement(element: IrElement) {
        if (!result) element.acceptChildrenVoid(this)
    }

    private fun IrElement.acceptAndGetResult(): Boolean {
        acceptVoid(this@NeedsToVisit)
        return result
    }

    private val IrClass.needsHandling: Boolean
        get() = isValue || typeParameters.any { it.acceptAndGetResult() }
    private val IrType.needsHandling: Boolean
        get() = classifierOrNull?.isBound == true && erasedUpperBound.needsHandling || this is IrSimpleType && arguments.any { it.typeOrNull?.needsHandling == true }

    override fun visitClass(declaration: IrClass) {
        visitClassHeader(declaration)
        if (result) return
        visitClassBody(declaration)
    }

    private fun visitClassBody(declaration: IrClass) {
        if (result) return
        result = replacements.classResults.getOrPut(Body to declaration) {
            declaration.declarations.any { it.acceptAndGetResult() }
        }
    }

    private fun visitClassHeader(declaration: IrClass) {
        if (result) return
        result = replacements.classResults.getOrPut(Header to declaration) {
            declaration.needsHandling ||
                    declaration.typeParameters.any { it.acceptAndGetResult() } ||
                    declaration.superTypes.mapNotNull { (it.classifierOrNull as? IrClassSymbol)?.owner }.any { visitClassHeader(it); result }
        }
    }

    override fun visitFunction(declaration: IrFunction) = visitFunction(declaration, withBody = true)

    private fun visitFunctionHeader(declaration: IrFunction) {
        if (result) return
        result = replacements.functionResults.getOrPut(Header to declaration) {
            declaration.parent.let { it is IrClass && it.needsHandling } ||
                    declaration.typeParameters.any { it.acceptAndGetResult() } ||
                    declaration.dispatchReceiverParameter?.acceptAndGetResult() == true ||
                    declaration.extensionReceiverParameter?.acceptAndGetResult() == true ||
                    declaration.valueParameters.any { it.acceptAndGetResult() } ||
                    declaration.returnType.needsHandling ||
                    (declaration as? IrSimpleFunction)?.overriddenSymbols?.any { visitFunction(it.owner, withBody = false); result } == true
        }
    }

    private fun visitFunction(declaration: IrFunction, withBody: Boolean) {
        visitFunctionHeader(declaration)
        if (result) return
        if(withBody) visitFunctionBody(declaration)
    }

    private fun visitFunctionBody(declaration: IrFunction) {
        if (result) return
        result = replacements.functionResults.getOrPut(Body to declaration) {
            declaration.body?.acceptAndGetResult() == true
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        if (result || !visitedParameters.add(declaration.symbol)) return
        result = declaration.type.needsHandling
        if (result) return
        super.visitValueParameter(declaration)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        if (result || !visitedParameters.add(declaration.symbol)) return
        result = declaration.superTypes.any { it.needsHandling }
        if (result) return
        super.visitTypeParameter(declaration)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        if (result) return
        visitFunction(expression.symbol.owner, withBody = false)
        if (result) return
        super.visitFunctionReference(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        if (result) return
        visitFunction(expression.symbol.owner, withBody = false)
        if (result) return
        super.visitFunctionAccess(expression)
    }

    override fun visitField(declaration: IrField) = visitField(declaration, withBody = true)

    private fun visitFieldHeader(declaration: IrField) {
        if (result) return
        result = replacements.fieldResults.getOrPut(Header to declaration) {
            declaration.type.needsHandling
        }
    }

    private fun visitFieldBody(declaration: IrField) {
        if (result) return
        result = replacements.fieldResults.getOrPut(Body to declaration) {
            super.visitField(declaration)
            result
        }
    }

    private fun visitField(declaration: IrField, withBody: Boolean) {
        visitFieldHeader(declaration)
        if (withBody) visitFieldBody(declaration)
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        if (result) return
        visitField(expression.symbol.owner, withBody = false)
        if (result) return
        super.visitFieldAccess(expression)
    }

    override fun visitVariable(declaration: IrVariable) {
        if (result) return
        result = declaration.type.needsHandling
        if (result) return
        super.visitVariable(declaration)
    }


    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        if (result) return
        result = expression.arguments.any { it.type.needsHandling }
        if (result) return
        super.visitStringConcatenation(expression)
    }

    override fun visitCall(expression: IrCall) {
        if (result) return
        if (expression.symbol == context.irBuiltIns.eqeqSymbol) {
            for (it in 0 until expression.valueArgumentsCount) {
                result = expression.getValueArgument(it)?.type?.needsHandling ?: false
                if (result) return
            }
        }
        super.visitCall(expression)
    }

    override fun visitExpression(expression: IrExpression) {
        if (result) return
        result = expression.type.needsHandling
        if (result) return
        super.visitExpression(expression)
    }
}
