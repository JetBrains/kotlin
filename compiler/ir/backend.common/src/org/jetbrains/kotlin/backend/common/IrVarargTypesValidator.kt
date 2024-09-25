/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.types.Variance

/**
 * Makes sure that: [IrVararg.type] is an array of [IrVararg.varargElementType].
 */
internal class IrVarargTypesValidator(
    private val irBuiltIns: IrBuiltIns,
    private val file: IrFile,
    private val config: IrValidatorConfig,
    private val reportError: ReportIrValidationError,
) : IrElementVisitorVoid {
    private val parentChain = mutableListOf<IrElement>()

    override fun visitElement(element: IrElement) {
        parentChain.push(element)
        element.acceptChildrenVoid(this)
        parentChain.pop()
    }

    override fun visitVararg(expression: IrVararg, data: Nothing?) {
        super.visitVararg(expression, data)
        validate(expression, expression.type, expression.varargElementType)
    }

    override fun visitValueParameter(parameter: IrValueParameter, data: Nothing?) {
        super.visitValueParameter(parameter, data)
        parameter.varargElementType?.let {
            validate(parameter, parameter.type, it)
        }
    }

    private fun validate(
        irElement: IrElement,
        type: IrType,
        varargElementType: IrType,
    ) {
        val isCorrectArrayOf = (type.isArray() || type.isNullableArray())
                && (type as IrSimpleType).arguments.single().let {
            when (it) {
                is IrSimpleType -> it == varargElementType
                is IrTypeProjection -> it.variance == Variance.OUT_VARIANCE && it.type == varargElementType
                else -> false
            }
        }
        if (isCorrectArrayOf) return

        val primitiveOrUnsignedElementType = type.classifierOrNull?.let { classifier ->
            irBuiltIns.primitiveArrayElementTypes[classifier]
                ?: irBuiltIns.unsignedArraysElementTypes[classifier]
        }
        val isCorrectArrayOfPrimitiveOrUnsigned = primitiveOrUnsignedElementType?.let { it == varargElementType }
        if (isCorrectArrayOfPrimitiveOrUnsigned == true) return

        reportError(
            file,
            irElement,
            "Vararg type=${type.render()} is expected to be an array of its underlying varargElementType=${varargElementType.render()}",
            parentChain
        )
    }
}
