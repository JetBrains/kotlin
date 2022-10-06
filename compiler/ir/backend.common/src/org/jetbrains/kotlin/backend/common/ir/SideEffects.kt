/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class SideEffects(val level: Int, val name: String) {

    /**
     * Aka 'pure'. [ReadNone] expressions can be reordered or eliminated.
     */
    object ReadNone : SideEffects(0, "READNONE")

    /**
     * May read the global state, may not alter it. Calls to [ReadOnly] functions cannot be reordered, but can be eliminated.
     */
    object ReadOnly : SideEffects(1, "READONLY")

    /**
     * A special kind of effect. Used to mark singleton initializers that are otherwise would be considered [ReadWrite],
     * but don't have any effects except saving the instance to a global variable.
     *
     * [otherwise] is the effects that a function would have if we removed all the [AlmostPureSingletonConstructor] effects from it.
     */
    data class AlmostPureSingletonConstructor(val otherwise: SideEffects) :
        SideEffects(2 + otherwise.level, "ALMOST_PURE_SINGLETON_CONSTRUCTOR")
    {
        init {
            require(otherwise !is AlmostPureSingletonConstructor)
        }
    }

    /**
     * Can arbitrarily alter the global state.
     */
    object ReadWrite : SideEffects(Int.MAX_VALUE, "READWRITE")

    fun isAtMost(other: SideEffects) = level <= other.level

    companion object {
        fun valueOf(s: String) = when (s) {
            ReadNone.name -> ReadNone
            ReadOnly.name -> ReadOnly
            ReadWrite.name -> ReadWrite
            else -> throw IllegalArgumentException("$s is not a valid side effect name")
        }
    }
}

private val effectsAnnotationFqName = FqName("kotlin.internal.Effects")
private val effectEnumFqName = FqName("kotlin.internal.Effect")

fun IrFunction.getDeclaredEffects(): SideEffects? {
    val effectsAnnotation = getAnnotation(effectsAnnotationFqName) ?: return null
    val arg = effectsAnnotation.getValueArgument(0) ?: compilationException("Missing argument in @Effects annotation", this)
    val enumEntry = arg.safeAs<IrDeclarationReference>()?.symbol?.safeAs<IrEnumEntrySymbol>()?.owner
        ?: compilationException("The argument of @Effects declaration should be an enum entry", arg)
    return SideEffects.valueOf(enumEntry.name.identifier)
}

fun IrFunction.addEffectsAnnotation(effects: SideEffects, context: CommonBackendContext) {
    if (effects is SideEffects.AlmostPureSingletonConstructor) {
        error("${effects.name} cannot be set via an annotation!")
    }
    val annotationClassSymbol = context.getClassSymbol(effectsAnnotationFqName)
    val enumClassSymbol = context.getClassSymbol(effectEnumFqName)
    val constructorSymbol = annotationClassSymbol.constructors.single()
    val entry = enumClassSymbol.owner.declarations.first { it is IrEnumEntry && it.name.asString() == effects.name } as IrEnumEntry
    @Suppress("SuspiciousCollectionReassignment")
    annotations += with(context.createIrBuilder(symbol)) {
        irCall(constructorSymbol).apply {
            putValueArgument(0, IrGetEnumValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, enumClassSymbol.typeWith(), entry.symbol))
        }
    }
}

typealias FunctionSideEffectMemoizer = MutableMap<IrFunctionSymbol, SideEffects>

fun IrExpression?.computeEffects(
    anyVariableReadIsPure: Boolean,
    memoizer: FunctionSideEffectMemoizer = mutableMapOf(),
    context: CommonBackendContext? = null,
): SideEffects =
    this?.accept(EffectAnalyzer(anyVariableReadIsPure, memoizer, context), Unit) ?: SideEffects.ReadNone

fun IrFunction.computeEffects(
    anyVariableReadIsPure: Boolean,
    memoizer: FunctionSideEffectMemoizer = mutableMapOf(),
    context: CommonBackendContext? = null,
): SideEffects {
    val analyzer = EffectAnalyzer(anyVariableReadIsPure, memoizer, context)
    analyzer.callStack.push(symbol)
    return computeEffectsImpl(analyzer)
}

private fun IrFunction.computeEffectsImpl(
    analyzer: EffectAnalyzer,
) = analyzer.memoizer.getOrPut(symbol) {
    if (analyzer.context != null && this is IrConstructor) {
        if (symbol.owner.constructedClass.symbol == analyzer.context.irBuiltIns.anyClass) {
            return@getOrPut SideEffects.ReadNone
        }
    }
    val effects = getDeclaredEffects()
        ?: body?.accept(analyzer, Unit)
        ?: SideEffects.ReadWrite

    // TODO: Good heuristic for Kotlin/JS, but will it work on other backends?
    if (effects is SideEffects.AlmostPureSingletonConstructor && this !is IrConstructor)
        return effects.otherwise

    effects
}

fun IrExpression?.isPure(anyVariableReadIsPure: Boolean) = computeEffects(anyVariableReadIsPure) == SideEffects.ReadNone

private inline fun <T> Iterable<T>.maxEffect(computeEffects: (T) -> SideEffects): SideEffects {
    val iterator = iterator()
    if (!iterator.hasNext()) return SideEffects.ReadNone
    var maxValue = computeEffects(iterator.next())
    while (iterator.hasNext()) {
        if (maxValue is SideEffects.ReadWrite) return maxValue
        val v = computeEffects(iterator.next())
        maxValue = maxEffectOf(maxValue, v)
    }
    return maxValue
}

private fun maxEffectOf(a: SideEffects, b: SideEffects): SideEffects {
    if (a is SideEffects.ReadWrite || b is SideEffects.ReadWrite) return SideEffects.ReadWrite

    if (a is SideEffects.AlmostPureSingletonConstructor && b is SideEffects.AlmostPureSingletonConstructor) {
        return SideEffects.AlmostPureSingletonConstructor(maxEffectOf(a.otherwise, b.otherwise))
    }

    if (a is SideEffects.AlmostPureSingletonConstructor) {
        return if (a.otherwise.level < b.level) SideEffects.AlmostPureSingletonConstructor(b) else a
    }

    if (b is SideEffects.AlmostPureSingletonConstructor) {
        return maxEffectOf(b, a)
    }

    return if (a.level >= b.level) a else b
}

private class EffectAnalyzer(
    val anyVariableReadIsPure: Boolean,
    val memoizer: FunctionSideEffectMemoizer,
    val context: CommonBackendContext?,
) : IrElementVisitor<SideEffects, Unit> {

    val callStack = mutableListOf<IrFunctionSymbol>()

    private fun <T : IrElement> Iterable<T>.maxEffectWithThis(): SideEffects {
        return maxEffect { it.accept(this@EffectAnalyzer, Unit) }
    }

    override fun visitElement(element: IrElement, data: Unit): SideEffects {
        return SideEffects.ReadWrite
    }

    override fun visitExpression(expression: IrExpression, data: Unit): SideEffects {
        return SideEffects.ReadWrite
    }

    override fun visitFunction(declaration: IrFunction, data: Unit): SideEffects {
        // Function declarations themselves have no effects.
        return SideEffects.ReadNone
    }

    override fun visitClass(declaration: IrClass, data: Unit): SideEffects {
        return SideEffects.ReadNone
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Unit): SideEffects {
        return SideEffects.ReadNone
    }

    override fun visitVariable(declaration: IrVariable, data: Unit): SideEffects {
        return declaration.initializer?.accept(this, data) ?: SideEffects.ReadNone
    }

    override fun visitBody(body: IrBody, data: Unit): SideEffects {
        return SideEffects.ReadWrite
    }

    override fun visitBlockBody(body: IrBlockBody, data: Unit): SideEffects {
        return body.statements.maxEffectWithThis()
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: Unit): SideEffects {
        return body.expression.accept(this, data)
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Unit): SideEffects {
        return when (body.kind) {
            IrSyntheticBodyKind.ENUM_VALUES -> SideEffects.ReadNone
            IrSyntheticBodyKind.ENUM_VALUEOF -> SideEffects.ReadNone
            IrSyntheticBodyKind.ENUM_ENTRIES -> SideEffects.ReadNone
        }
    }

    override fun visitBranch(branch: IrBranch, data: Unit): SideEffects {
        return listOf(branch.condition, branch.result).maxEffectWithThis()
    }

    override fun visitCatch(aCatch: IrCatch, data: Unit): SideEffects {
        return listOf(aCatch.catchParameter, aCatch.result).maxEffectWithThis()
    }

    override fun visitConst(expression: IrConst<*>, data: Unit): SideEffects {
        return SideEffects.ReadNone
    }

    override fun visitGetValue(expression: IrGetValue, data: Unit): SideEffects {
        if (anyVariableReadIsPure) return SideEffects.ReadNone
        val valueDeclaration = expression.symbol.owner
        val isPure = if (valueDeclaration is IrVariable) !valueDeclaration.isVar
        else true
        return if (isPure) SideEffects.ReadNone else SideEffects.ReadWrite
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Unit): SideEffects {
        return if (expression.operator !in setOf(IrTypeOperator.INSTANCEOF, IrTypeOperator.REINTERPRET_CAST, IrTypeOperator.NOT_INSTANCEOF))
            SideEffects.ReadWrite
        else
            expression.argument.computeEffects(anyVariableReadIsPure)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Unit): SideEffects {
        return expression.symbol.owner.primaryConstructor?.accept(this, data) ?: SideEffects.ReadNone
    }

    override fun visitGetField(expression: IrGetField, data: Unit): SideEffects {
        if (!expression.symbol.owner.isFinal && !anyVariableReadIsPure) {
            return SideEffects.ReadWrite
        }
        return expression.receiver?.accept(this, data) ?: SideEffects.ReadOnly
    }

    override fun visitSetField(expression: IrSetField, data: Unit): SideEffects {
        val valueEffect = expression.value.accept(this, data)
        if (valueEffect == SideEffects.ReadWrite) return SideEffects.ReadWrite

        val constructorSymbol = callStack.lastOrNull() as? IrConstructorSymbol? ?: return SideEffects.ReadWrite

        if (expression.symbol.owner.origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE) {
            return SideEffects.AlmostPureSingletonConstructor(valueEffect)
        }

        // If we are in a constructor, and we're setting a constructed instance's field, treat it as a READNONE operation.
        val receiver = expression.receiver as? IrGetValue ?: return SideEffects.ReadWrite
        val valueParameter = receiver.symbol.owner as? IrValueParameter ?: return SideEffects.ReadWrite
        if (!valueParameter.isDispatchReceiver) return SideEffects.ReadWrite
        val assignmentEffect =
            if (valueParameter.parent == constructorSymbol.owner.constructedClass) SideEffects.ReadNone else SideEffects.ReadWrite
        return maxEffectOf(valueEffect, assignmentEffect)
    }

    override fun visitVararg(expression: IrVararg, data: Unit): SideEffects {
        return expression.elements.maxEffect {
            (it as? IrExpression)?.accept(this, data) ?: SideEffects.ReadWrite
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Unit): SideEffects {
        val function = expression.symbol.owner

        if (callStack.contains(function.symbol)) {
            // Consider recursive calls non-pure
            // A more precise analysis can be done, but for now we stick with this.
            return SideEffects.ReadWrite
        }

        callStack.push(function.symbol)

        try {
            val functionSideEffects = function.computeEffectsImpl(this)

            if (functionSideEffects == SideEffects.ReadWrite) return SideEffects.ReadWrite

            val argComputationSideEffects = (0 until expression.valueArgumentsCount).maxEffect {
                expression.getValueArgument(it)!!.accept(this, data)
            }

            return maxEffectOf(functionSideEffects, argComputationSideEffects)
        } finally {
            callStack.pop()
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: Unit): SideEffects {
        return expression.statements.maxEffectWithThis()
    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: Unit): SideEffects {
        return SideEffects.ReadNone
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: Unit): SideEffects {
        return SideEffects.ReadNone
    }

    override fun visitWhen(expression: IrWhen, data: Unit): SideEffects {
        return expression.branches.maxEffectWithThis()
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Unit): SideEffects {
        return expression.arguments.maxEffectWithThis()
    }

    override fun visitReturn(expression: IrReturn, data: Unit): SideEffects {
        return expression.value.accept(this, data)
    }

    override fun visitThrow(expression: IrThrow, data: Unit): SideEffects {
        return expression.value.accept(this, data)
    }

    override fun visitLoop(loop: IrLoop, data: Unit): SideEffects {
        return listOfNotNull(loop.condition, loop.body).maxEffectWithThis()
    }

    override fun visitTry(aTry: IrTry, data: Unit): SideEffects {
        return buildList {
            add(aTry.tryResult)
            addAll(aTry.catches)
            aTry.finallyExpression?.let { add(it) }
        }.maxEffectWithThis()
    }
}