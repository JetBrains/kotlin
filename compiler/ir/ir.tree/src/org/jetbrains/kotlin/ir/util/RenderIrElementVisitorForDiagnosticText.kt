/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.types.classOrFail

/**
 * This class can be used to beautifully print IR elements for compiler diagnostics, unlike
 * the original [RenderIrElementVisitor] which is used in tests for rendering tree dumps.
 */
class RenderIrElementVisitorForDiagnosticText private constructor() : RenderIrElementVisitor() {
    override fun visitClassReference(expression: IrClassReference, data: Nothing?): String {
        val className = expression.classType.classOrFail.owner.name
        return "$className::class"
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): String {
        val className = expression.type.classOrFail.owner.name
        val entryName = expression.symbol.owner.name
        return "$className.$entryName"
    }

    companion object {
        fun renderAsAnnotation(annotation: IrConstructorCall): String {
            return RenderIrElementVisitorForDiagnosticText().renderAsAnnotation(annotation)
        }
    }
}
