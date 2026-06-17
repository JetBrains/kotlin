/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import kotlin.collections.forEach

/** NOTE: The order and names are important. */
enum class EffectsKind {
    PURE,
    READ,
    WRITE,
}

/**
 * Each function (and element that can be impure) has this cell attached to it via the [IrElement.effects] attribute.
 * The attribute is set in EffectAnalysisLowering.
 *
 * The effects of one function can depend on another; a dependency can be established before
 * the dependent's effects are fully resolved. This means that we can't just use a plain enum value
 * here, and instead have to keep track of dependencies and resolve them lazily.
 *
 * After effect analysis is finished though, we can cache the computed value because the dependencies won't change.
 *
 * There are two kinds of cells. `Exact` cells just have a constant value and are used when a function is annotated with `@Effects`.
 * The `Lazy` cells have dependencies as described above.
 */
sealed interface EffectsKindCell {
    fun compute(): EffectsKind

    class Exact(val exact: EffectsKind) : EffectsKindCell {
        override fun compute() = exact
    }

    class Lazy(val context: JsCommonBackendContext, val function: IrFunction, val owner: IrElement) : EffectsKindCell {
        private var minimum = EffectsKind.PURE

        private val dependencies = hashSetOf<Lazy>()

        private var cachedValue: EffectsKind? = null

        private var frozen = false

        fun freeze() {
            frozen = true
        }

        private fun compute(visited: HashSet<EffectsKindCell>): EffectsKind {
            if (visited.contains(this)) return EffectsKind.PURE
            visited.add(this)
            var result = minimum
            for (dependency in dependencies) {
                val effects = dependency.compute(visited)
                if (effects > result) result = effects
            }
            return result
        }

        override fun compute(): EffectsKind = if (context.effectAnalysisFinished) {
            if (cachedValue == null) {
                cachedValue = compute(hashSetOf())
            }
            cachedValue!!
        } else {
            compute(hashSetOf())
        }

        fun dependOn(cell: EffectsKindCell) {
            if (frozen) throw IllegalStateException("Called dependOn on frozen cell")
            if (cell == this) return
            when (cell) {
                is Exact -> setAtLeast(cell.exact)
                is Lazy -> if (cell.frozen) {
                    setAtLeast(cell.minimum)
                    dependencies.addAll(cell.dependencies)
                } else {
                    dependencies.add(cell)
                }
            }
        }

        fun setAtLeast(kind: EffectsKind) {
            if (frozen) throw IllegalStateException("Called setAtLeast on frozen cell")
            if (kind > minimum) {
                minimum = kind
            }
        }
    }
}

class ExpressionEffectVisitor : IrVisitorVoid() {
    var result = EffectsKind.PURE

    override fun visitElement(element: IrElement) {
        element.effects?.let {
            val effects = it.compute()
            if (effects > result) result = effects
            // we don't visit the children here because of how EffectAnalysisLowering sets these effects.
            // (it visits the children for us)
            return
        }
        element.acceptChildrenVoid(this)
    }
}

fun IrExpression.computeEffectsKind(): EffectsKind {
    val v = ExpressionEffectVisitor()
    acceptVoid(v)
    return v.result
}

object EffectAnalysisClassIds {
    val annotation = ClassId(StandardClassIds.BASE_INTERNAL_PACKAGE, Name.identifier("Effects"))
    val kindParameter = Name.identifier("kind")
}
