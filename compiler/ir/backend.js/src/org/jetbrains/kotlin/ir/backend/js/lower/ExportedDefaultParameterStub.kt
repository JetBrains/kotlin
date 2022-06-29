/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.hasStableJsName
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName

class ExportedDefaultParameterStub(val context: JsIrBackendContext) : DeclarationTransformer {

    private fun IrBuilderWithScope.createDefaultResolutionExpression(
        fromParameter: IrValueParameter,
        toParameter: IrValueParameter,
    ): IrExpression? {
        return fromParameter.defaultValue?.let { defaultValue ->
            irIfThenElse(
                toParameter.type,
                irEqeqeq(
                    irGet(toParameter, context.irBuiltIns.anyNType),
                    irCall(this@ExportedDefaultParameterStub.context.intrinsics.jsUndefined)
                ),
                defaultValue.expression,
                irGet(toParameter)
            )
        }
    }

    private fun IrConstructor.introduceDefaultResolution(): IrConstructor {
        val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)

        val variables = mutableMapOf<IrValueParameter, IrValueDeclaration>()

        val defaultResolutionStatements = valueParameters.mapNotNull { valueParameter ->
            irBuilder.createDefaultResolutionExpression(valueParameter, valueParameter)?.let { initializer ->
                JsIrBuilder.buildVar(
                    valueParameter.type,
                    this@introduceDefaultResolution,
                    name = valueParameter.name.asString(),
                    initializer = initializer
                ).also {
                    variables[valueParameter] = it
                }
            }
        }

        if (variables.isNotEmpty()) {
            body?.transformChildren(VariableRemapper(variables), null)

            body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                statements += defaultResolutionStatements
                statements += body?.statements ?: emptyList()
            }
        }


        return also {
            valueParameters.forEach {
                if (it.defaultValue != null) {
                    it.origin = JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER
                }
            }
        }
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrFunction) {
            return null
        }

        if (!declaration.hasStableJsName(context)) {
            return null
        }

        if (!declaration.valueParameters.any { it.defaultValue != null }) {
            return null
        }

        if (declaration is IrConstructor) {
            return listOf(declaration.introduceDefaultResolution())
        }

        val exportedDefaultStubFun = declaration.generateExportedDefaultArgumentSubWithoutBody(context).apply {
            body = context.createIrBuilder(symbol, startOffset, endOffset).irBlockBody(this) {
                +irReturn(irCall(declaration).apply {
                    passTypeArgumentsFrom(declaration)
                    dispatchReceiver = dispatchReceiverParameter?.let { irGet(it) }
                    extensionReceiver = extensionReceiverParameter?.let { irGet(it) }

                    declaration.valueParameters.forEachIndexed { index, irValueParameter ->
                        val exportedParameter = valueParameters[index]
                        val value = createDefaultResolutionExpression(irValueParameter, exportedParameter) ?: irGet(exportedParameter)
                        putValueArgument(index, value)
                    }
                })
            }
        }

        if (declaration.isExported(context)) {
            context.additionalExportedDeclarations.add(exportedDefaultStubFun)
        }

        declaration.origin = JsLoweredDeclarationOrigin.JS_SHADOWED_EXPORT

        val (exportAnnotations, irrelevantAnnotations) = declaration.annotations.map { it.deepCopyWithSymbols(declaration as? IrDeclarationParent) }
            .partition {
                it.isAnnotationWithEqualFqName(JsAnnotations.jsExportFqn) || it.isAnnotationWithEqualFqName(JsAnnotations.jsNameFqn)
            }

        declaration.annotations = irrelevantAnnotations
        exportedDefaultStubFun.annotations = exportAnnotations

        return listOf(exportedDefaultStubFun, declaration)
    }
}

fun IrFunction.generateExportedDefaultArgumentSubWithoutBody(context: JsIrBackendContext): IrSimpleFunction {
    val exportedDefaultStubFun = context.irFactory.buildFun {
        updateFrom(this@generateExportedDefaultArgumentSubWithoutBody)
        name = this@generateExportedDefaultArgumentSubWithoutBody.name
        origin = JsIrBuilder.SYNTHESIZED_DECLARATION
    }

    exportedDefaultStubFun.parent = parent

    exportedDefaultStubFun.copyParameterDeclarationsFrom(this)

    exportedDefaultStubFun.valueParameters.forEach {
        if (it.defaultValue != null) {
            it.origin = JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER
        }
        it.defaultValue = null
    }

    exportedDefaultStubFun.returnType = returnType.remapTypeParameters(this, exportedDefaultStubFun)

    return exportedDefaultStubFun
}

