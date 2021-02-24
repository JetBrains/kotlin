/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.hasStableJsName
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName

private fun IrConstructorCall.isAnnotation(name: FqName): Boolean {
    return symbol.owner.parentAsClass.fqNameWhenAvailable == name
}

class ExportedDefaultParameterStub(val context: JsIrBackendContext) : DeclarationTransformer {

    private fun IrBuilderWithScope.createDefaultResolutionExpression(
        fromParameter: IrValueParameter,
        toParameter: IrValueParameter,
    ): IrExpression? {
        return fromParameter.defaultValue?.let { defaultValue ->
            irIfThenElse(
                toParameter.type,
                irEqeqeq(
                    irGet(toParameter),
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


        return this
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

        val exportedDefaultStubFun = context.irFactory.buildFun {
            updateFrom(declaration)
            name = declaration.name
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }

        context.additionalExportedDeclarations.add(exportedDefaultStubFun)

        exportedDefaultStubFun.parent = declaration.parent
        exportedDefaultStubFun.copyParameterDeclarationsFrom(declaration)
        exportedDefaultStubFun.returnType = declaration.returnType.remapTypeParameters(declaration, exportedDefaultStubFun)
        exportedDefaultStubFun.valueParameters.forEach { it.defaultValue = null }

        declaration.origin = JsLoweredDeclarationOrigin.JS_SHADOWED_EXPORT

        val irBuilder = context.createIrBuilder(exportedDefaultStubFun.symbol, exportedDefaultStubFun.startOffset, exportedDefaultStubFun.endOffset)
        exportedDefaultStubFun.body = irBuilder.irBlockBody(exportedDefaultStubFun) {
            +irReturn(irCall(declaration).apply {
                passTypeArgumentsFrom(declaration)
                dispatchReceiver = exportedDefaultStubFun.dispatchReceiverParameter?.let { irGet(it) }
                extensionReceiver = exportedDefaultStubFun.extensionReceiverParameter?.let { irGet(it) }

                declaration.valueParameters.forEachIndexed { index, irValueParameter ->
                    val exportedParameter = exportedDefaultStubFun.valueParameters[index]
                    val value = createDefaultResolutionExpression(irValueParameter, exportedParameter) ?: irGet(exportedParameter)
                    putValueArgument(index, value)
                }
            })
        }

        val (exportAnnotations, irrelevantAnnotations) = declaration.annotations.map { it.deepCopyWithSymbols(declaration as? IrDeclarationParent) }
            .partition {
                it.isAnnotation(JsAnnotations.jsExportFqn) || (it.isAnnotation(JsAnnotations.jsNameFqn))
            }

        declaration.annotations = irrelevantAnnotations
        exportedDefaultStubFun.annotations = exportAnnotations

        return listOf(exportedDefaultStubFun, declaration)
    }
}

