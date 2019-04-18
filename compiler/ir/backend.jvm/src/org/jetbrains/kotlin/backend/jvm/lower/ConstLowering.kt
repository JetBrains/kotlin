/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val constPhase = makeIrFilePhase(
    ::ConstLowering,
    name = "Const",
    description = "Substitute calls to const properties with constant values"
)

fun IrField.constantValue(implicitConst: Boolean = false): IrConst<*>? {
    // javac always inlines reads of static final fields, so do that for ones imported from Java.
    // Kotlin fields are only inlined if explicitly `const` to avoid making the values part of the ABI.
    val inline = correspondingPropertySymbol?.owner?.isConst == true ||
            (isStatic && isFinal && (origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB || implicitConst))
    return if (inline) initializer?.expression as? IrConst<*> else null
}

class ConstLowering(val context: CommonBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val function = (expression.symbol.owner as? IrSimpleFunction) ?: return super.visitCall(expression)
        val property = function.correspondingPropertySymbol?.owner ?: return super.visitCall(expression)
        if (function != property.getter)
            return super.visitCall(expression)
        return property.backingField?.constantValue() ?: super.visitCall(expression)
    }

    override fun visitGetField(expression: IrGetField): IrExpression =
        expression.symbol.owner.constantValue() ?: super.visitGetField(expression)
}
