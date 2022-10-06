/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.types.impl.IrUnionType

class CollectFunctionUsagesLowering(context: JsIrBackendContext) : BodyLoweringPass {
    private val transformer = CollectFunctionUsagesLoweringVisitor(context)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(transformer)
    }
}

private class CollectFunctionUsagesLoweringVisitor(val context: JsIrBackendContext) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        return super.visitCall(expression).also {
            val calls = context.optimizations.functionUsages.getOrPut(expression.symbol) {
                mutableSetOf()
            }
            calls.add(expression)
        }
    }
}

class FunctionSpecializationLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && !declaration.isExported(context)) {
            declaration.valueParameters.forEachIndexed { i, it ->
                it.type = specializedTypeFor(declaration, i)
            }
        }
        return null
    }

    private fun specializedTypeFor(function: IrSimpleFunction, parameterIndex: Int): IrType {
        val previousType = function.valueParameters[parameterIndex].type
        val usages = context.optimizations.functionUsages[function.symbol] ?: return previousType

        if (usages.isEmpty()) return previousType

        val allCollectedTypes = usages.asSequence()
            .map {
                it.getValueArgument(parameterIndex) ?: IrConstImpl.constNull(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.nothingNType
                )
            }
            .map { it.type }
            .toSet()


        return IrUnionType(allCollectedTypes, previousType.annotations)
    }
}

