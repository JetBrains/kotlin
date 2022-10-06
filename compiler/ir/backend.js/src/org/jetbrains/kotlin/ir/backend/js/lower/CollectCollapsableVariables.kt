/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.lower.inline.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.isSimpleProperty

class CollectSimpleVariables(context: JsIrBackendContext) : BodyLoweringPass {
    private val transformer = CollectSimpleVariablesVisitor(context)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(transformer)
    }
}

private class CollectSimpleVariablesVisitor(val context: JsIrBackendContext) : IrElementTransformerVoid() {
    private val simpleVariables = context.optimizations.collapsableVariablesValues
    private val variablesBlackList = mutableSetOf<CollapsableElements>()
    private val variablesGetExpressions = mutableSetOf<CollapsableElements>()

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (declaration is IrVariable) {
            val initializer = declaration.initializer

            if (initializer != null) {
                simpleVariables[declaration.symbol] = initializer
            }
        }

        return super.visitDeclaration(declaration)
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        val symbol = expression.symbol as? IrVariableSymbol ?: return super.visitSetValue(expression)
        val collapsableElement = CollapsableElements.CollapsableValue(symbol)

        if (symbol in simpleVariables) {
            variablesBlackList.add(collapsableElement)
            simpleVariables.remove(symbol)
        } else if (collapsableElement !in variablesBlackList) {
            simpleVariables[symbol] = expression.value
        }

        return super.visitSetValue(expression)
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        val symbol = expression.symbol
        val collapsableElement = CollapsableElements.CollapsableField(symbol)

        if (symbol in simpleVariables) {
            variablesBlackList.add(collapsableElement)
            simpleVariables.remove(symbol)
        } else if (
            collapsableElement !in variablesBlackList &&
            symbol.owner.correspondingPropertySymbol.shouldCollapse() &&
            expression.value.isReachable()
        ) {
            simpleVariables[symbol] = expression.value
        }

        return super.visitSetField(expression)
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val symbol = expression.symbol
        val collapsableElement = CollapsableElements.CollapsableField(symbol)

        if (symbol in simpleVariables) {
            if (collapsableElement in variablesGetExpressions && !expression.collapse(context).isSimpleValue()) {
                variablesBlackList.add(collapsableElement)
                simpleVariables.remove(symbol)
            } else if (collapsableElement !in variablesBlackList) {
                variablesGetExpressions.add(collapsableElement)
            }
        }

        return super.visitGetField(expression)
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val symbol = expression.symbol as? IrVariableSymbol ?: return super.visitGetValue(expression)
        val collapsableElement = CollapsableElements.CollapsableValue(symbol)

        if (symbol in simpleVariables) {
            if (collapsableElement in variablesGetExpressions && !expression.collapse(context).isSimpleValue()) {
                variablesBlackList.add(collapsableElement)
                simpleVariables.remove(symbol)
            } else if (collapsableElement !in variablesBlackList) {
                variablesGetExpressions.add(collapsableElement)
            }
        }

        return super.visitGetValue(expression)
    }

    private fun IrExpression.isSimpleValue(): Boolean {
        return this is IrGetValue ||
                this is IrRawFunctionReference ||
                (this is IrCall && symbol.isUnitInstanceFunction()) ||
                (this is IrGetField && receiver == null)
    }

    private fun IrExpression.isReachable(): Boolean {
        return this !is IrGetValue &&
                (this !is IrGetField || receiver?.isReachable() != false) &&
                this !is IrCall &&
                this !is IrConstructorCall
    }


    private fun IrFunctionSymbol.isUnitInstanceFunction(): Boolean {
        return owner.origin === JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION &&
                owner.returnType.classifierOrNull === context.irBuiltIns.unitClass
    }

    private fun IrPropertySymbol?.shouldCollapse(): Boolean =
        this != null && owner.isSimpleProperty && !owner.isExported(context)
}


