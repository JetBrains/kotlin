/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.memoryOptimizedMap

private object SCRIPT_FUNCTION : IrDeclarationOriginImpl("SCRIPT_FUNCTION")

class CreateScriptFunctionsPhase(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.declarations.transformFlat { declaration ->
            if (declaration is IrScript) lower(declaration)
            else null
        }
    }

    private fun lower(irScript: IrScript): List<IrDeclaration> {
        val (startOffset, endOffset) = getFunctionBodyOffsets(irScript)

        val initializeStatements = irScript.statements
            .asSequence()
            .filterIsInstance<IrProperty>()
            .mapNotNull { it.backingField }
            .filter { it.initializer != null }
            .map { Pair(it, it.initializer!!.expression) }
            .onEach { it.first.initializer = null }
            .toList()

        val initializeScriptFunction = createFunction(irScript, "\$initializeScript\$", context.irBuiltIns.unitType).also {
            it.body = it.factory.createBlockBody(
                startOffset,
                endOffset,
                initializeStatements.let {
                    if (irScript.resultProperty == null || initializeStatements.lastOrNull()?.first?.correspondingPropertySymbol != irScript.resultProperty) it
                    else it.dropLast(1)
                }.memoryOptimizedMap { (field, expression) -> createIrSetField(field, expression) }
            )
        }

        val evaluateScriptFunction = createFunction(irScript, "\$evaluateScript\$", getReturnType(irScript)).also {
            it.body = it.factory.createBlockBody(
                startOffset,
                endOffset,
                irScript.statements.filter { it !is IrDeclaration }
                    .let {
                        val lastInitializer = initializeStatements.lastOrNull()
                        if (lastInitializer == null || irScript.resultProperty == null
                            || lastInitializer.first.correspondingPropertySymbol != irScript.resultProperty) {
                            it
                        } else {
                            it + lastInitializer.second
                        }
                    }.prepareForEvaluateScriptFunction(it)
            )
        }

        with(irScript) {
            statements.removeIf { it !is IrDeclaration }
            statements += initializeScriptFunction
            initializeScriptFunction.patchDeclarationParents(this)
            statements += evaluateScriptFunction
            evaluateScriptFunction.patchDeclarationParents(this)
            statements += createCall(initializeScriptFunction)
            statements += createCall(evaluateScriptFunction)
        }

        return listOf(irScript)
    }

    private fun getFunctionBodyOffsets(irScript: IrScript): Pair<Int, Int> {
        return with(irScript.statements) {
            if (isEmpty()) {
                Pair(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            } else {
                Pair(irScript.statements.first().startOffset, irScript.statements.last().endOffset)
            }
        }
    }

    private fun getReturnType(irScript: IrScript): IrType {
        return (irScript.statements.lastOrNull() as? IrExpression)?.type ?: context.irBuiltIns.unitType
    }

    private fun createFunction(irScript: IrScript, name: String, returnType: IrType): IrSimpleFunction =
        context.irFactory.buildFun {
            val (startOffset, endOffset) = getFunctionBodyOffsets(irScript)
            this.startOffset = startOffset
            this.endOffset = endOffset
            this.origin = SCRIPT_FUNCTION
            this.name = Name.identifier(name)
            this.visibility = DescriptorVisibilities.PRIVATE
            this.returnType = returnType
        }.also {
            it.parent = irScript
        }

    private fun List<IrStatement>.prepareForEvaluateScriptFunction(evaluateScriptFunction: IrFunction): List<IrStatement> {
        return if (isNotEmpty()) {
            val returnStatement = IrReturnImpl(
                last().startOffset,
                last().endOffset,
                context.irBuiltIns.nothingType,
                evaluateScriptFunction.symbol,
                last() as IrExpression
            )
            dropLast(1) + returnStatement
        } else emptyList()
    }

    private fun createIrSetField(field: IrField, expression: IrExpression): IrSetField {
        return IrSetFieldImpl(
            field.startOffset,
            field.endOffset,
            field.symbol,
            null,
            expression,
            expression.type
        )
    }

    private fun createCall(function: IrSimpleFunction): IrCall {
        return IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, function.returnType,
            function.symbol,
            valueArgumentsCount = function.valueParameters.size,
            typeArgumentsCount = function.typeParameters.size
        )
    }
}
