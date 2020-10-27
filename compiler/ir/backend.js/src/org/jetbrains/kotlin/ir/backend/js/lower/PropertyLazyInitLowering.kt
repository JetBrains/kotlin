/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class PropertyLazyInitLowering(private val context: JsIrBackendContext) : FileLoweringPass {
    private val irBuiltIns
        get() = context.irBuiltIns

    override fun lower(irFile: IrFile) {
        val initializers = PropertyInitializerMover(context).process(irFile)
        val irFactory = context.irFactory
        if (initializers.isNotEmpty()) {
            irFactory.addFunction(irFile) {
                name = Name.identifier("init properties ${irFile.kotlinFqName}")
                returnType = irBuiltIns.unitType
                origin = JsIrBuilder.SYNTHESIZED_DECLARATION
            }.apply {
                body = irFactory.createBlockBody(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    initializers
                        .map { (field, expression) ->
                            createIrSetField(field, expression)
                        }
                )
            }
        }
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
}

private class PropertyInitializerMover(
    private val context: JsIrBackendContext
) : IrElementTransformerVoid() {

    private val fieldToInitializers = mutableListOf<Pair<IrField, IrExpression>>()

    fun process(irFile: IrFile): List<Pair<IrField, IrExpression>> {
        irFile.transformChildrenVoid(this)
        return fieldToInitializers
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        declaration.correspondingPropertySymbol
            ?.owner
            ?.takeIf { !it.isConst }
            ?.takeIf { !it.isDelegated }
            ?.backingField
            ?.takeIf { it.initializer != null }
            ?.let { field ->
                fieldToInitializers.add(field to field.initializer!!.expression)
            }

        fieldToInitializers.forEach { it.first.initializer = null }

        return super.visitSimpleFunction(declaration)
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression): IrExpression {
        return super.visitFieldAccess(expression)
    }

    override fun visitField(declaration: IrField): IrStatement {
        return super.visitField(declaration)
    }

    override fun visitBody(body: IrBody): IrBody {
        return super.visitBody(body)
    }
}