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

enum class SideEffects {

    /**
     * Aka 'pure'. [READNONE] expressions can be reordered or eliminated.
     */
    READNONE,

    /**
     * May read the global state, may not alter it. Calls to [READONLY] functions cannot be reordered, but can be eliminated.
     */
    READONLY,

    /**
     * A special kind of effect. Used to mark singleton initializers that are otherwise would be considered [READWRITE],
     * but don't have any effects except saving the instance to a global variable.
     */
    ALMOST_PURE_SINGLETON_CONSTRUCTOR,

    /**
     * Can arbitrarily alter the global state.
     */
    READWRITE,
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
    if (effects == SideEffects.ALMOST_PURE_SINGLETON_CONSTRUCTOR) {
        error("${SideEffects.ALMOST_PURE_SINGLETON_CONSTRUCTOR.name} cannot be set via an annotation!")
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
    this?.accept(EffectAnalyzer(anyVariableReadIsPure, memoizer, context), Unit) ?: SideEffects.READNONE

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
            return@getOrPut SideEffects.READNONE
        }
    }
    getDeclaredEffects()
        ?: body?.accept(analyzer, Unit)
        ?: SideEffects.READWRITE
}

fun IrExpression?.isPure(anyVariableReadIsPure: Boolean) = computeEffects(anyVariableReadIsPure) == SideEffects.READNONE

private inline fun <T> Iterable<T>.maxEffect(computeEffects: (T) -> SideEffects): SideEffects {
    return maxOfOrNull {
        computeEffects(it).also { result ->
            // Early exit to avoid expensive computations if we already know that we're going to get READWRITE.
            if (result == SideEffects.READWRITE) return SideEffects.READWRITE
        }
    } ?: SideEffects.READNONE
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
        return SideEffects.READWRITE
    }

    override fun visitExpression(expression: IrExpression, data: Unit): SideEffects {
        return SideEffects.READWRITE
    }

    override fun visitFunction(declaration: IrFunction, data: Unit): SideEffects {
        // Function declarations themselves have no effects.
        return SideEffects.READNONE
    }

    override fun visitClass(declaration: IrClass, data: Unit): SideEffects {
        return SideEffects.READNONE
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Unit): SideEffects {
        return SideEffects.READNONE
    }

    override fun visitVariable(declaration: IrVariable, data: Unit): SideEffects {
        return declaration.initializer?.accept(this, data) ?: SideEffects.READNONE
    }

    override fun visitBody(body: IrBody, data: Unit): SideEffects {
        return SideEffects.READWRITE
    }

    override fun visitBlockBody(body: IrBlockBody, data: Unit): SideEffects {
        return body.statements.maxEffectWithThis()
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: Unit): SideEffects {
        return body.expression.accept(this, data)
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Unit): SideEffects {
        return when (body.kind) {
            IrSyntheticBodyKind.ENUM_VALUES -> SideEffects.READNONE
            IrSyntheticBodyKind.ENUM_VALUEOF -> SideEffects.READNONE
            IrSyntheticBodyKind.ENUM_ENTRIES -> SideEffects.READNONE
        }
    }

    override fun visitBranch(branch: IrBranch, data: Unit): SideEffects {
        return listOf(branch.condition, branch.result).maxEffectWithThis()
    }

    override fun visitCatch(aCatch: IrCatch, data: Unit): SideEffects {
        return listOf(aCatch.catchParameter, aCatch.result).maxEffectWithThis()
    }

    override fun visitConst(expression: IrConst<*>, data: Unit): SideEffects {
        return SideEffects.READNONE
    }

    override fun visitGetValue(expression: IrGetValue, data: Unit): SideEffects {
        if (anyVariableReadIsPure) return SideEffects.READNONE
        val valueDeclaration = expression.symbol.owner
        val isPure = if (valueDeclaration is IrVariable) !valueDeclaration.isVar
        else true
        return if (isPure) SideEffects.READNONE else SideEffects.READWRITE
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Unit): SideEffects {
        return if (expression.operator !in setOf(IrTypeOperator.INSTANCEOF, IrTypeOperator.REINTERPRET_CAST, IrTypeOperator.NOT_INSTANCEOF))
            SideEffects.READWRITE
        else
            expression.argument.computeEffects(anyVariableReadIsPure)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Unit): SideEffects {
        return expression.symbol.owner.primaryConstructor?.accept(this, data) ?: SideEffects.READNONE
    }

    override fun visitGetField(expression: IrGetField, data: Unit): SideEffects {
        if (!expression.symbol.owner.isFinal && !anyVariableReadIsPure) {
            return SideEffects.READWRITE
        }
        return expression.receiver?.accept(this, data) ?: SideEffects.READONLY
    }

    override fun visitSetField(expression: IrSetField, data: Unit): SideEffects {
        val valueEffect = expression.value.accept(this, data)
        if (valueEffect == SideEffects.READWRITE) return SideEffects.READWRITE

        val constructorSymbol = callStack.lastOrNull() as? IrConstructorSymbol? ?: return SideEffects.READWRITE

        if (expression.symbol.owner.origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE) {
            return SideEffects.ALMOST_PURE_SINGLETON_CONSTRUCTOR
        }

        // If we are in a constructor, and we're setting a constructed instance's field, treat it as a READNONE operation.
        val receiver = expression.receiver as? IrGetValue ?: return SideEffects.READWRITE
        val valueParameter = receiver.symbol.owner as? IrValueParameter ?: return SideEffects.READWRITE
        if (!valueParameter.isDispatchReceiver) return SideEffects.READWRITE
        val assignmentEffect =
            if (valueParameter.parent == constructorSymbol.owner.constructedClass) SideEffects.READNONE else SideEffects.READWRITE
        return maxOf(valueEffect, assignmentEffect)
    }

    override fun visitVararg(expression: IrVararg, data: Unit): SideEffects {
        return expression.elements.maxEffect {
            (it as? IrExpression)?.accept(this, data) ?: SideEffects.READWRITE
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Unit): SideEffects {
        val function = expression.symbol.owner

        if (callStack.contains(function.symbol)) {
            // Consider recursive calls non-pure
            // A more precise analysis can be done, but for now we stick with this.
            return SideEffects.READWRITE
        }

        callStack.push(function.symbol)

        try {
            val functionSideEffects = function.computeEffectsImpl(this)

            if (functionSideEffects == SideEffects.READWRITE) return SideEffects.READWRITE

            val argComputationSideEffects = (0 until expression.valueArgumentsCount).maxEffect {
                expression.getValueArgument(it)!!.accept(this, data)
            }

            return maxOf(functionSideEffects, argComputationSideEffects)
        } finally {
            callStack.pop()
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: Unit): SideEffects {
        return expression.statements.maxEffectWithThis()
    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: Unit): SideEffects {
        return SideEffects.READNONE
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: Unit): SideEffects {
        return SideEffects.READNONE
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