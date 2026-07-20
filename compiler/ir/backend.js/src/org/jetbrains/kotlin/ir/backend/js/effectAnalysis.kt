/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import java.lang.ref.WeakReference

/** NOTE: The order and names are important. */
enum class EffectsKind {
    PURE,
    READ,
    WRITE,
}

/**
 * Each function has this cell attached to it via the [IrFunction.effects] attribute.
 * The attribute is set in EffectAnalysisLowering.
 *
 * The effects of one function can depend on another; a dependency can be established before
 * the dependent's effects are fully resolved. This means that we can't just use a plain enum value
 * here, and instead have to keep track of dependencies and resolve them lazily.
 *
 * After effect analysis is finished though, we can cache the computed value because the dependencies won't change.
 */
class EffectsKindCell(val context: JsCommonBackendContext, val owner: WeakReference<IrFunction>, val exact: EffectsKind?) {
    private var minimum = EffectsKind.PURE

    // We use weak references here because there can be loops.
    private val dependencies = hashSetOf<WeakReference<EffectsKindCell>>()

    private var cachedValue: EffectsKind? = null

    private fun compute(visited: HashSet<EffectsKindCell>): EffectsKind {
        if (exact != null) return exact
        if (visited.contains(this)) return EffectsKind.PURE
        visited.add(this)
        var result = minimum
        for (dependency in dependencies) {
            val cell = dependency.get() ?: continue
            val effects = cell.compute(visited)
            if (effects > result) result = effects
        }
        return result
    }

    fun compute(): EffectsKind = if (context.effectAnalysisFinished) {
        if (cachedValue == null) {
            cachedValue = compute(hashSetOf())
        }
        cachedValue!!
    } else {
        compute(hashSetOf())
    }

    fun dependOn(cell: EffectsKindCell) {
        if (cell == this) return
        if (cell.exact != null) {
            setAtLeast(cell.exact)
            return
        }
        dependencies.add(WeakReference(cell))
        cell.dependencies.asSequence().mapNotNull { it.get()?.exact }.forEach { setAtLeast(it) }
    }

    fun setAtLeast(kind: EffectsKind) {
        if (kind > minimum) {
            minimum = kind
        }
    }
}

class ExpressionEffectVisitor(val function: IrFunction?) : IrVisitorVoid() {
    var result = EffectsKind.PURE

    override fun visitExpression(expression: IrExpression) {
        expression.acceptChildrenVoid(this)
    }

    override fun visitGetField(expression: IrGetField) {
        super.visitGetField(expression)
        // pending not having field accesses during this phase
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        super.visitFunctionAccess(expression)
        val effects = expression.symbol.owner.effects?.compute() ?: EffectsKind.WRITE
        if (effects > result) result = effects
    }
}

fun IrExpression.computeEffectsKind(function: IrFunction?): EffectsKind {
    val v = ExpressionEffectVisitor(function)
    acceptVoid(v)
    return v.result
}
