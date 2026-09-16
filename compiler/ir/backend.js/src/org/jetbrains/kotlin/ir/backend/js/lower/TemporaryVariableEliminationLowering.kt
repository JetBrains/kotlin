/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.EffectAnalysisClassIds
import org.jetbrains.kotlin.ir.backend.js.EffectsKind
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.effects
import org.jetbrains.kotlin.ir.backend.js.enableEffectAnalysis
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import kotlin.collections.hashMapOf

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
 *
 * Notes:
 * - We need to keep track of vals assigned late.
 * - We need to keep track of assignments to variables used in the initalizer of variables that might be substituted.
 *   For example in `var x = 2; val a = x; x += 2; x = x + a`, we can't eliminate `a` because x is written to.
 *   This is a sort-of optimization of internal effects.
 *
 *
 * Terminology:
 * - _Source._ Variable eligible for elimination, its declaration and initializer (which can be a late assignment),
 *           and how many times it is used in the function.
 * - _Tracked variable._ A variable that might be eliminated. Similar to sources, but in this case
 *                       we keep track of the effects of the initializer (init-effects),
 *                       and the effects that happened after the initializer (post-effects).
 * - Note that effects are a possibly-empty set of a `read` effect and a `write` effect.
 *   The enum `PURE` corresponds to `{}`, `READ` to `{read}`, and `WRITE` to `{read,write}`
 *
 * First, we collect:
 * - The sources. (`sources : Symbol → Source`)
 * - For each variable in the function, in which initializers it is mentioned. (`mentioned : Symbol → Set<Symbol>`)
 *
 * For late initializer, we need to make sure that the result of the expression is not used.
 *
 * We set up a set of tracked variables
 * (`tracked : Symbol → (init-effects : Effects, post-effects : Effect, initializer: Expression)`).
 *
 * Then we go over the code again, generating a set of eliminated tracked variables (`eliminated : Symbol → Expression`):
 * - When we encounter a use of a tracked variable:
 *   - This means that this variable will be eliminated in the next pass (because we only track variables with one use).
 *   - We can stop tracking this variable, and add it to the eliminated set.
 *   - We propagate the init-effects of the initializer up the stack (1).
 * - When we encounter a "global" effect (from effect analysis):
 *   - We propagate the effect up the stack (2).
 *   - We go over all tracked variables:
 *     - We update the post-effects of the tracked variable
 *     - If _post-effects ∩ init-effects ≠ ∅_, then we stop tracking the variable.
 * - When we encounter the initializer of a source:
 *   - If the number of usages isn't 1, we don't do anything special.
 *   - Then we visit the initializer, keeping of the effects that we got from (1) and (2), adding them to the init-effects.
 *   - We add the variable to the set of tracked variables.
 * - When we encounder a mutation of a variable in `mentioned`:
 *   - We stop tracking any variables in the set.
 *
 *
 * Finally, we transform the ir, replacing the uses of variables in `eliminated` with the initializers,
 * and removing the declarations and late assignments.
 */
class TemporaryVariableEliminationLowering(val context: JsCommonBackendContext) : ChangeAwareBodyLoweringPass {
    var debug = false

    override fun changeAwareLower(irBody: IrBody, container: IrDeclaration): Boolean {
        if (container !is IrFunction) return false
        debug = container.name.asString() == "box"
        val collector = Collector()
        irBody.accept(collector, CollectingMode())
        val visitor = Visitor(collector.usages, collector.mentioned, container)
        irBody.accept(visitor, null)
        val transformer = Transformer(visitor.eliminated)
        irBody.transformChildren(transformer, null)
        return transformer.hadChanges
    }


    inline fun IrElement.computeEffectsOr(or: () -> EffectsKind): EffectsKind {
        val effects = this@computeEffectsOr.effects
        return if (!context.configuration.enableEffectAnalysis || effects == null) {
            or()
        } else {
            effects.compute()
        }
    }

    class TrackedVariable(
        val initializer: IrExpression,
        /** The effects of the initializer expression. (!= PURE) */
        val initEffects: EffectsKind,
        /** The effects that happened after the initializer for this variable. */
        var postEffects: EffectsKind = EffectsKind.PURE,
    )

    private sealed class Source(var count: Int = 0) {
        class Uninitialized : Source()

        class Declaration : Source()

        /** Late assignment (e.g. `val x; x = 4`). */
        class Assignment(val setValue: IrSetValue) : Source()
    }

    // needed to ensure we treat things like loops correctly.
    private class CollectingMode(
        val initializers: MutableList<IrSymbol> = mutableListOf(),
        /** Loops, when's. */
        var isInNonlinearControlFlow: Boolean = false,
    ) {
        inline fun nonlinear(f: () -> Unit) {
            val old = isInNonlinearControlFlow
            isInNonlinearControlFlow = true
            f()
            isInNonlinearControlFlow = old
        }
    }

    // counts how many usages a variable has.
    private inner class Collector : IrVisitor<Unit, CollectingMode>() {
        val usages = hashMapOf<IrSymbol, Source>()
        val banned = hashSetOf<IrSymbol>()

        // any variable that is used inside an initializer (or late assignment).
        // e.g. `val a = x + y; val b = x + z;` gives { x -> {a, b}, y -> {a}, z -> {b} }
        val mentioned = hashMapOf<IrSymbol, HashSet<IrSymbol>>()

        override fun visitElement(element: IrElement, data: CollectingMode) {
            element.acceptChildren(this, data)
        }

        override fun visitVariable(declaration: IrVariable, data: CollectingMode) {
            if (!data.isInNonlinearControlFlow && !declaration.isVar) {
                val source = if (declaration.initializer == null) Source.Uninitialized() else Source.Declaration()
                usages[declaration.symbol] = source
                data.initializers.add(declaration.symbol)
                declaration.acceptChildren(this, data)
                data.initializers.pop()
                return
            }
            declaration.acceptChildren(this, data)
        }

        override fun visitWhen(expression: IrWhen, data: CollectingMode) {
            if (expression.branches.isEmpty()) return

            // the first branch condition is always executed
            expression.branches[0].condition.accept(this, data)

            data.nonlinear {
                expression.branches[0].result.accept(this, data)
                expression.branches.asSequence().drop(1).forEach { it.accept(this, data) }
            }
        }

        override fun visitLoop(loop: IrLoop, data: CollectingMode) {
            data.nonlinear {
                loop.acceptChildren(this, data)
            }
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: CollectingMode) {
            expression.acceptChildren(this, data)
            if (data.isInNonlinearControlFlow) usages.remove(expression.symbol)
            else usages[expression.symbol]?.let { it.count += 1 }
        }

        override fun visitGetValue(expression: IrGetValue, data: CollectingMode) {
            if (data.isInNonlinearControlFlow) usages.remove(expression.symbol)
            else usages[expression.symbol]?.let { it.count += 1 }

            data.initializers.forEach {
                mentioned.computeIfAbsent(expression.symbol) { hashSetOf() }.add(it)
            }
        }

        override fun visitSetValue(expression: IrSetValue, data: CollectingMode) {
            if (!data.isInNonlinearControlFlow) {
                val owner = expression.symbol.owner
                if (owner is IrVariable) {
                    // late val assignments
                    if (!owner.isVar) {
                        // only a single assignment is supported.
                        if (usages[owner.symbol].let { it == null || it !is Source.Uninitialized }) {
                            usages.remove(owner.symbol)
                            banned.add(owner.symbol)
                        } else if (owner.symbol !in banned) {
                            val source = Source.Assignment(expression)
                            usages[owner.symbol] = source
                            data.initializers.add(owner.symbol)
                            expression.acceptChildren(this, data)
                            data.initializers.pop()
                            return
                        }
                    }
                }
            }
            expression.acceptChildren(this, data)
        }
    }

    // the main Visitor does the same.
    // this is used when we only need mutation checking.
    private inner class MutationChecker(
        val tracking: HashMap<IrSymbol, TrackedVariable>,
        val mentioned: HashMap<IrSymbol, HashSet<IrSymbol>>,
        val effects: (EffectsKind) -> Unit,
        val function: IrElement,
    ) : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitSetValue(expression: IrSetValue) {
            effects(expression.computeEffectsOr {
                if (expression.symbol.owner.parent == function) EffectsKind.PURE else EffectsKind.WRITE
            })
            mentioned[expression.symbol]?.let { mentions ->
                mentions.forEach { tracking.remove(it) }
            }
            expression.acceptChildrenVoid(this)
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
            expression.acceptChildrenVoid(this)
            effects(expression.computeEffectsOr {
                val owner = expression.symbol.owner
                val effectsAnnotation = owner.getAnnotation(EffectAnalysisClassIds.annotation.asSingleFqName())
                if (effectsAnnotation != null) {
                    val arg = effectsAnnotation.argumentMapping[EffectAnalysisClassIds.kindParameter]
                    if (arg is IrGetEnumValue) {
                        return@computeEffectsOr EffectsKind.valueOf(arg.symbol.owner.name.asString())
                    }
                }
                EffectsKind.WRITE
            })
        }

        override fun visitGetValue(expression: IrGetValue) {
            effects(expression.computeEffectsOr {
                if (expression.symbol.owner.parent == function) EffectsKind.PURE else EffectsKind.READ
            })
        }

        override fun visitGetField(expression: IrGetField) {
            expression.acceptChildrenVoid(this)
            effects(expression.computeEffectsOr { EffectsKind.READ })
        }

        override fun visitSetField(expression: IrSetField) {
            expression.acceptChildrenVoid(this)
            effects(expression.computeEffectsOr { EffectsKind.WRITE })
        }
    }

    private inner class Visitor(
        val usages: HashMap<IrSymbol, Source>,
        val mentioned: HashMap<IrSymbol, HashSet<IrSymbol>>,
        val function: IrElement,
    ) : IrVisitor<Unit, ((EffectsKind) -> Unit)?>() {
        // we track the variables that might be eliminated.
        val tracking = hashMapOf<IrSymbol, TrackedVariable>()
        val eliminated = hashMapOf<IrSymbol, IrExpression?>()

        fun barrier(propagate: ((EffectsKind) -> Unit)?, effects: EffectsKind) {
            if (effects == EffectsKind.PURE) return
            propagate?.invoke(effects)
            val iter = tracking.values.iterator()
            for (tracked in iter) {
                if (effects > tracked.postEffects) {
                    tracked.postEffects = effects
                    // if we know that the variable can't be eliminated after this point we stop tracking it.
                    if (tracked.initEffects == EffectsKind.READ && tracked.postEffects == EffectsKind.WRITE) {
                        iter.remove()
                    }
                    if (tracked.initEffects == EffectsKind.WRITE && tracked.postEffects != EffectsKind.PURE) {
                        iter.remove()
                    }
                }
            }
        }

        override fun visitElement(element: IrElement, data: ((EffectsKind) -> Unit)?) {
            element.acceptChildren(this, data)
        }

        override fun visitSetValue(expression: IrSetValue, data: ((EffectsKind) -> Unit)?) {
            mentioned[expression.symbol]?.let { mentions ->
                mentions.forEach { tracking.remove(it) }
            }
            expression.acceptChildren(this, data)
        }

        override fun visitWhen(expression: IrWhen, data: ((EffectsKind) -> Unit)?) {
            if (expression.branches.isEmpty()) return

            // the first branch condition is always executed, so we can eliminate variables there
            expression.branches[0].condition.accept(this, data)

            // in the rest of the expression we can't eliminate, but we still need to check for mutations and effects.
            var effects = EffectsKind.PURE
            val mutationChecker = MutationChecker(tracking, mentioned, {
                if (it > effects) effects = it
            }, function)
            expression.branches[0].result.acceptVoid(mutationChecker)
            expression.branches.asSequence().drop(1).forEach { it.acceptVoid(mutationChecker) }
            barrier(data, effects)
        }

        override fun visitLoop(loop: IrLoop, data: ((EffectsKind) -> Unit)?) {
            // we can't eliminate variables inside loops, so we only check for mutations and effects.
            var effects = EffectsKind.PURE
            loop.acceptChildrenVoid(MutationChecker(tracking, mentioned, {
                if (it > effects) effects = it
            }, function))
            barrier(data, effects)
        }

        override fun visitVariable(declaration: IrVariable, data: ((EffectsKind) -> Unit)?) {
            usages[declaration.symbol]?.also { source ->
                if (source.count > 1) return super.visitVariable(declaration, data)
                if (source.count == 0) {
                    super.visitVariable(declaration, data)
                    eliminated[declaration.symbol] = null
                    return
                }
                val initializer = when (source) {
                    // this can happen if the error is suppressed.
                    is Source.Uninitialized ->
                        IrCompositeImpl(
                            declaration.startOffset,
                            declaration.endOffset,
                            declaration.type
                        )
                    is Source.Declaration -> declaration.initializer!!
                    is Source.Assignment -> source.setValue.value
                }
                var initEffects = EffectsKind.PURE
                super.visitVariable(declaration) { effects ->
                    if (effects > initEffects) initEffects = effects
                    data?.invoke(effects)
                }
                tracking[declaration.symbol] = TrackedVariable(initializer, initEffects)
            } ?: super.visitVariable(declaration, data)
        }

        fun maybeEliminate(expression: IrDeclarationReference, propagate: ((EffectsKind) -> Unit)?) {
            tracking[expression.symbol]?.let { tracked ->
                propagate?.let { it(tracked.initEffects) }
                tracking.remove(expression.symbol)
                eliminated[expression.symbol] = tracked.initializer
            }
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: ((EffectsKind) -> Unit)?) {
            maybeEliminate(expression, data)
            expression.acceptChildren(this, data)
            barrier(data, expression.computeEffectsOr {
                val owner = expression.symbol.owner
                val effectsAnnotation = owner.getAnnotation(EffectAnalysisClassIds.annotation.asSingleFqName())
                if (effectsAnnotation != null) {
                    val arg = effectsAnnotation.argumentMapping[EffectAnalysisClassIds.kindParameter]
                    if (arg is IrGetEnumValue) {
                        return@computeEffectsOr EffectsKind.valueOf(arg.symbol.owner.name.asString())
                    }
                }
                EffectsKind.WRITE
            })
        }

        override fun visitGetValue(expression: IrGetValue, data: ((EffectsKind) -> Unit)?) {
            maybeEliminate(expression, data)
            barrier(data, expression.computeEffectsOr {
                if (expression.symbol.owner.parent == function) EffectsKind.PURE else EffectsKind.READ
            })
        }

        override fun visitGetField(expression: IrGetField, data: ((EffectsKind) -> Unit)?) {
            expression.acceptChildren(this, data)
            barrier(data, expression.computeEffectsOr { EffectsKind.READ })
        }

        override fun visitSetField(expression: IrSetField, data: ((EffectsKind) -> Unit)?) {
            expression.acceptChildren(this, data)
            barrier(data, expression.computeEffectsOr { EffectsKind.WRITE })
        }
    }

    private inner class Transformer(val eliminated: Map<IrSymbol, IrExpression?>) : IrTransformer<Nothing?>() {
        var hadChanges = false

        fun empty(of: IrElement, type: IrType? = null) =
            IrCompositeImpl(of.startOffset, of.endOffset, type ?: context.irBuiltIns.unitType) // add origin here?

        override fun visitVariable(declaration: IrVariable, data: Nothing?): IrStatement {
            if (declaration.symbol in eliminated) {
                hadChanges = true
                eliminated[declaration.symbol]?.let {
                    return empty(declaration)
                }
                return declaration.initializer?.transform(this, null) ?: empty(declaration)
            }

            return super.visitVariable(declaration, data)
        }

        override fun visitSetValue(expression: IrSetValue, data: Nothing?): IrExpression {
            eliminated[expression.symbol]?.let {
                hadChanges = true
                return empty(expression, expression.type)
            }
            return super.visitSetValue(expression, data)
        }

        override fun visitGetValue(expression: IrGetValue, data: Nothing?): IrExpression {
            eliminated[expression.symbol]?.let {
                hadChanges = true
                return it.transform(this, null)
            }
            return super.visitGetValue(expression, data)
        }
    }
}
