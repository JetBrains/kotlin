package org.jetbrains.kotlin.backend.konan.lower.loops

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.lower.matchers.IrCallMatcher
import org.jetbrains.kotlin.backend.konan.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.backend.konan.lower.matchers.createIrCallMatcher
import org.jetbrains.kotlin.backend.konan.lower.matchers.singleArgumentExtension
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class RangeToHandler(val progressionElementClasses: Collection<IrClassSymbol>) : ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        dispatchReceiver { it != null && it.type.classifierOrNull in progressionElementClasses }
        fqName { it.pathSegments().last() == Name.identifier("rangeTo") }
        parameterCount { it == 1 }
        parameter(0) { it.type.classifierOrNull in progressionElementClasses }
    }

    override fun build(call: IrCall, progressionType: ProgressionType) =
            ProgressionInfo(progressionType, call.dispatchReceiver!!, call.getValueArgument(0)!!)
}

internal class DownToHandler(val progressionElementClasses: Collection<IrClassSymbol>) : ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.downTo"), progressionElementClasses)
        parameter(0) { it.type.classifierOrNull in progressionElementClasses }
    }

    override fun build(call: IrCall, progressionType: ProgressionType): ProgressionInfo? =
            ProgressionInfo(progressionType,
                    call.extensionReceiver!!,
                    call.getValueArgument(0)!!,
                    increasing = false)
}

internal class UntilHandler(val progressionElementClasses: Collection<IrClassSymbol>) : ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.until"), progressionElementClasses)
        parameter(0) { it.type.classifierOrNull in progressionElementClasses }
    }

    override fun build(call: IrCall, progressionType: ProgressionType): ProgressionInfo? =
            ProgressionInfo(progressionType,
                    call.extensionReceiver!!,
                    call.getValueArgument(0)!!,
                    closed = false)
}

internal class IndicesHandler(val context: Context) : ProgressionHandler {

    private val symbols = context.ir.symbols

    private val supportedArrays = symbols.primitiveArrays.values + symbols.array

    override val matcher = createIrCallMatcher {
        callee {
            fqName { it == FqName("kotlin.collections.<get-indices>") }
            parameterCount { it == 0 }
        }
        extensionReceiver { it != null && it.type.classifierOrNull in supportedArrays }
    }

    override fun build(call: IrCall, progressionType: ProgressionType): ProgressionInfo? {
        val int0 = IrConstImpl.int(call.startOffset, call.endOffset, context.irBuiltIns.intType, 0)

        val bound = with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            val clazz = call.extensionReceiver!!.type.classifierOrFail
            val symbol = symbols.arraySize[clazz]!!
            irCall(symbol).apply {
                dispatchReceiver = call.extensionReceiver
            }
        }
        return ProgressionInfo(progressionType, int0, bound, closed = false)
    }
}

internal class StepHandler(context: Context, val visitor: IrElementVisitor<ProgressionInfo?, Nothing?>) : ProgressionHandler {
    private val symbols = context.ir.symbols

    override val matcher: IrCallMatcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.step"), symbols.progressionClasses)
        parameter(0) { it.type.isInt() || it.type.isLong() }
    }

    override fun build(call: IrCall, progressionType: ProgressionType): ProgressionInfo? {
        val nestedInfo = call.extensionReceiver!!.accept(visitor, null)
                ?: return null

        // Due to KT-27607 nested non-default steps could lead to incorrect behaviour.
        // So disable optimization of such rare cases for now.
        if (nestedInfo.step != null) {
            return null
        }

        val newStep = call.getValueArgument(0)!!
        val (newStepCheck, needBoundCalculation) = irCheckProgressionStep(symbols, progressionType, newStep)
        return ProgressionInfo(
                progressionType,
                nestedInfo.first,
                nestedInfo.bound,
                newStepCheck, nestedInfo.increasing,
                needBoundCalculation,
                nestedInfo.closed)
    }

    private fun IrConst<*>.stepIsOne() = when (kind) {
        IrConstKind.Long -> value as Long == 1L
        IrConstKind.Int -> value as Int == 1
        else -> false
    }

    private fun IrExpression.isPositiveConst() = this is IrConst<*> &&
            ((kind == IrConstKind.Long && value as Long > 0) || (kind == IrConstKind.Int && value as Int > 0))

    // Used only by the assert.
    private fun stepHasRightType(step: IrExpression, progressionType: ProgressionType) = when (progressionType) {

        ProgressionType.CHAR_PROGRESSION,
        ProgressionType.INT_PROGRESSION -> step.type.makeNotNull().isInt()

        ProgressionType.LONG_PROGRESSION -> step.type.makeNotNull().isLong()
    }

    private fun irCheckProgressionStep(symbols: KonanSymbols, progressionType: ProgressionType, step: IrExpression) =
            if (step.isPositiveConst()) {
                step to !(step as IrConst<*>).stepIsOne()
            } else {
                // The frontend checks if the step has a right type (Long for LongProgression and Int for {Int/Char}Progression)
                // so there is no need to cast it.
                assert(stepHasRightType(step, progressionType))

                val symbol = symbols.checkProgressionStep[step.type.makeNotNull().toKotlinType()]
                        ?: throw IllegalArgumentException("No `checkProgressionStep` for type ${step.type}")
                IrCallImpl(step.startOffset, step.endOffset, symbol.owner.returnType, symbol).apply {
                    putValueArgument(0, step)
                } to true
            }
}
