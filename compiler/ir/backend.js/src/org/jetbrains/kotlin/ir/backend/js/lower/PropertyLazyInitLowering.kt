/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.INTERNAL
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isPure
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import kotlin.collections.component1
import kotlin.collections.component2

class PropertyLazyInitLowering(private val context: JsIrBackendContext) : FileLoweringPass {
    private val irBuiltIns
        get() = context.irBuiltIns

    private val calculator = JsIrArithBuilder(context)

    private val irFactory
        get() = context.irFactory

    override fun lower(irFile: IrFile) {
        val functions = TopLevelFunsSearcher()
            .search(irFile)

        val fieldToInitializer = calculateFieldToExpression(
            functions
        ).onEach { it.key.initializer = null }

        if (fieldToInitializer.isEmpty()) return

        val fileName = irFile.name
        val initialisedField = irFactory.createInitialisationField(fileName)
            .apply {
                irFile.declarations.add(this)
                parent = irFile
            }

        val initialisationFun = irFactory.addFunction(irFile) {
            name = Name.identifier("init properties $fileName")
            returnType = irBuiltIns.unitType
            visibility = INTERNAL
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }.apply {
            buildPropertiesInitializationBody(
                fieldToInitializer,
                initialisedField
            )
        }

        functions
            .asSequence()
            .filterNotNull()
            .forEach { function ->
                val newBody = function.body?.let { body ->
                    irFactory.bodyWithFunctionCall(body, initialisationFun)
                }
                function.body = newBody
            }
    }

    private fun IrFactory.createInitialisationField(fileName: String): IrField =
        buildField {
            name = Name.identifier("properties initialised $fileName")
            type = irBuiltIns.booleanType
            isStatic = true
            isFinal = true
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }

    private fun IrSimpleFunction.buildPropertiesInitializationBody(
        initializers: Map<IrField, IrExpression>,
        initialisedField: IrField
    ) {

        body = irFactory.createBlockBody(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            buildBodyWithIfGuard(initializers, initialisedField)
        )
    }

    private fun buildBodyWithIfGuard(
        initializers: Map<IrField, IrExpression>,
        initialisedField: IrField
    ): List<IrWhen> {
        val statements = initializers
            .map { (field, expression) ->
                createIrSetField(field, expression)
            }

        val upGuard = createIrSetField(
            initialisedField,
            JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
        )

        return JsIrBuilder.buildIfElse(
            type = irBuiltIns.unitType,
            cond = calculator.not(createIrGetField(initialisedField)),
            thenBranch = JsIrBuilder.buildComposite(
                type = irBuiltIns.unitType,
                statements = mutableListOf(upGuard).apply { addAll(statements) }
            )
        ).let { listOf(it) }
    }
}

private fun calculateFieldToExpression(functions: Collection<IrSimpleFunction>): Map<IrField, IrExpression> =
    functions
        .asSequence()
        .mapNotNull { it.correspondingPropertySymbol }
        .map { it.owner }
        .filter { it.isTopLevel }
        .filterNot { it.isConst }
        .distinct()
        .mapNotNull { it.backingField }
        .filter { it.initializer != null }
        .map { it to it.initializer!!.expression }
        .toMap()

private fun createIrGetField(field: IrField): IrGetField {
    return JsIrBuilder.buildGetField(
        symbol = field.symbol,
        receiver = null
    )
}

private fun createIrSetField(field: IrField, expression: IrExpression): IrSetField {
    return JsIrBuilder.buildSetField(
        symbol = field.symbol,
        receiver = null,
        value = expression,
        type = expression.type
    )
}

private fun IrFactory.bodyWithFunctionCall(
    body: IrBody,
    functionToCall: IrSimpleFunction
): IrBody = createBlockBody(
    body.startOffset,
    body.endOffset,
    mutableListOf<IrStatement>(
        JsIrBuilder.buildCall(
            target = functionToCall.symbol,
            type = functionToCall.returnType
        )
    ).apply { addAll(body.statements) }
)

private class TopLevelFunsSearcher : IrElementTransformerVoid() {

    private val topLevelFuns = mutableSetOf<IrSimpleFunction>()

    fun search(irFile: IrFile): Set<IrSimpleFunction> {
        irFile.transformChildrenVoid(this)
        return topLevelFuns
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {

        if (declaration.isTopLevel) {
            topLevelFuns.add(declaration)
        }

        return super.visitSimpleFunction(declaration)
    }
}