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
import org.jetbrains.kotlin.ir.types.isNullable

class SpeculationLowering(context: JsIrBackendContext) : BodyLoweringPass {
    private val transformer = SpeculationLoweringVisitor(context)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(transformer)
    }
}
private class SpeculationLoweringVisitor(val context: JsIrBackendContext) : IrElementTransformerVoid() {
    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        val result = super.visitStringConcatenation(expression)

        if (result is IrStringConcatenation && result.arguments.any { !it.type.isNullable() }) {
           result.type = context.irBuiltIns.stringType
        }
        return result
    }
}