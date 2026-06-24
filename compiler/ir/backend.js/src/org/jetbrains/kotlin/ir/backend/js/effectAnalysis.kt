/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/** NOTE: The order and names are important. */
enum class EffectsKind {
    PURE,
    READ,
    WRITE,
}

class EffectsKindCell(var stored: EffectsKind) {
    fun setMax(kind: EffectsKind) {
        if (kind > stored) {
            stored = kind
        }
    }
}

fun IrFunction.setMaxEffects(kind: EffectsKind?) {
    if (kind == null) return
    if (this.effects == null) {
        this.effects = EffectsKindCell(kind)
    } else {
        this.effects!!.setMax(kind)
    }
}

class ExpressionEffectVisitor : IrVisitorVoid() {
    var result = EffectsKind.PURE

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        super.visitFunctionAccess(expression)
        val effects = expression.symbol.owner.effects?.stored ?: EffectsKind.WRITE
        if (effects > result) result = effects
    }
}

fun IrExpression.computeEffectsKind(): EffectsKind {
    val v = ExpressionEffectVisitor()
    acceptVoid(v)
    return v.result
}
