/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.EffectsKind
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.computeEffectsKind
import org.jetbrains.kotlin.ir.backend.js.effects
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * This is a reimplementation of [org.jetbrains.kotlin.js.inline.clean.TemporaryVariableElimination],
 * but using the IR-level effect information.
 *
 * The lowering consists of three phases, as the original. Here, the first phase counts the number of usages
 * each variable has. Because we limit substitutions only to variables with one use (might change in the future, explicitly for
 * variables that have a value type and a pure initializer), this phase heavily simplifies the later phases.
 * The second phase goes over the body in evaluation order and ensures that impure substitutions don't cross read/write effect barriers.
 * The last phase does the actual substitution.
 * There might be a way to get rid of the last phase and do the substitutions in the second phase directly.
 */
class TemporaryVariableEliminationLowering(@Suppress("UNUSED") context: JsCommonBackendContext) : ChangeAwareBodyLoweringPass {
    override fun changeAwareLower(irBody: IrBody, container: IrDeclaration): Boolean {
        if (container !is IrFunction) return false
        val collector = Collector()
        irBody.acceptVoid(collector)
        val visitor = Visitor(collector.usages)
        irBody.accept(visitor, null)
        val transformer = Transformer(visitor.tracking)
        irBody.transformChildren(transformer, Unit)
        return transformer.hadChanges
    }

    class TrackedVariable(
        val initializer: IrExpression,
        /** The effects of the initializer expression. (!= PURE) */
        val initEffects: EffectsKind,
        /** The effects that happened after the initializer for this variable. */
        var postEffects: EffectsKind = EffectsKind.PURE,
    )

    // counts how many usages a variable has.
    class Collector : IrVisitorVoid() {
        val usages = hashMapOf<IrSymbol, Int>()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitVariable(declaration: IrVariable) {
            declaration.acceptChildrenVoid(this)
            if (!declaration.isVar && declaration.initializer != null) {
                usages[declaration.symbol] = 0
            }
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
            expression.acceptChildrenVoid(this)
            usages.computeIfPresent(expression.symbol) { _, v -> v + 1 }
        }

        override fun visitGetValue(expression: IrGetValue) {
            usages.computeIfPresent(expression.symbol) { _, v -> v + 1 }
        }
    }

    class Visitor(val usages: HashMap<IrSymbol, Int>) : IrVisitor<Unit, ((EffectsKind) -> Unit)?>() {
        // we track the variables that might be substituted.
        val tracking = hashMapOf<IrSymbol, TrackedVariable>()

        override fun visitElement(element: IrElement, data: ((EffectsKind) -> Unit)?) {
            element.effects?.let {
                val effects = it.compute()
                val iter = tracking.values.iterator()
                for (tracked in iter) {
                    if (effects > tracked.postEffects) {
                        tracked.postEffects = effects
                        // if we know that the variable can't be substituted after this point we stop tracking it.
                        if (tracked.initEffects == EffectsKind.READ && tracked.postEffects == EffectsKind.WRITE) {
                            iter.remove()
                        }
                        if (tracked.initEffects == EffectsKind.WRITE && tracked.postEffects != EffectsKind.PURE) {
                            iter.remove()
                        }
                    }
                }
            }
            element.acceptChildren(this, data)
        }

        override fun visitLoop(loop: IrLoop, data: ((EffectsKind) -> Unit)?) {
            // we ignore loops as they might evaluate the body and condition multiple times.
        }

        override fun visitVariable(declaration: IrVariable, data: ((EffectsKind) -> Unit)?) {
            usages[declaration.symbol]?.also { count ->
                if (count != 1) return
                val initializer = declaration.initializer!! // guaranteed non-null by Collector
                var initEffects = initializer.computeEffectsKind()
                // we need to keep track of the effects of the variables that will be substituted into this initialization, too.
                super.visitVariable(declaration) { effects ->
                    if (effects > initEffects) initEffects = effects
                }
                tracking[declaration.symbol] = TrackedVariable(initializer, initEffects)
                // note: we don't need to do the "can we track after this point" check (like we do in visitElement) here,
                //       because any "moves" (substitutions) that we do here can't affect the other tracked variables.
                // note: any given substitution *always* keeps the correct order,
                //       so any combination of substitutions keeps the correct order.
            } ?: super.visitVariable(declaration, null)
        }

        fun variableReference(expression: IrDeclarationReference, initializing: ((EffectsKind) -> Unit)?) {
            tracking[expression.symbol]?.let { tracked ->
                initializing?.let { it(tracked.initEffects) }
            }
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: ((EffectsKind) -> Unit)?) {
            variableReference(expression, data)
            super.visitFunctionAccess(expression, data)
        }

        override fun visitGetValue(expression: IrGetValue, data: ((EffectsKind) -> Unit)?) {
            variableReference(expression, data)
            super.visitGetValue(expression, data)
        }
    }

    class Transformer(val variables: Map<IrSymbol, TrackedVariable>) : IrTransformer<Unit>() {
        var hadChanges = false

        override fun visitVariable(declaration: IrVariable, data: Unit): IrStatement {
            variables[declaration.symbol]?.let {
                declaration.initializer = it.initializer.transform(this, Unit)
                hadChanges = true
                return IrCompositeImpl(declaration.startOffset, declaration.endOffset, declaration.type) // add origin here?
            }
            return super.visitVariable(declaration, data)
        }

        override fun visitGetValue(expression: IrGetValue, data: Unit): IrExpression {
            variables[expression.symbol]?.let {
                hadChanges = true
                return it.initializer
            }
            return super.visitGetValue(expression, data)
        }
    }
}
