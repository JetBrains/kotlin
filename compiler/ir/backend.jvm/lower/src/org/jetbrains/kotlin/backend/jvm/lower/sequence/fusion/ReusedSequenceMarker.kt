/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal var IrValueDeclaration.usageCounter: Int? by irAttribute(false)

internal class ReusedSequenceMarker(val context: JvmBackendContext) : IrVisitorVoid() {
    val sequences = mutableSetOf<IrVariable>()
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitGetValue(expression: IrGetValue) {
        if (sequences.any { it == expression.symbol.owner }) {
            expression.symbol.owner.usageCounter = (expression.symbol.owner.usageCounter ?: 0) + 1
        }
        super.visitGetValue(expression)
    }

    override fun visitVariable(declaration: IrVariable) {
        if (declaration.initializer != null && isElementSequence(context, declaration.initializer!!)) sequences.add(declaration)
        super.visitVariable(declaration)
    }
}
