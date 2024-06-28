/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.intrinsics

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.interpreter.exceptions.handleUserException
import org.jetbrains.kotlin.ir.interpreter.exceptions.stop
import org.jetbrains.kotlin.ir.interpreter.exceptions.withExceptionHandler
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KFunctionState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KTypeState
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.types.Variance
import java.util.*

internal sealed class IntrinsicBase {
    abstract fun getListOfAcceptableFunctions(): List<String>
    abstract fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment)
    open fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        return listOf(customEvaluateInstruction(irFunction, environment))
    }

    private fun customEvaluateInstruction(irFunction: IrFunction, environment: IrInterpreterEnvironment): CustomInstruction {
        return CustomInstruction {
            withExceptionHandler(environment) { // Exception handling is used only for indent actions; TODO: drop later
                evaluate(irFunction, environment)
                environment.callStack.dropFrameAndCopyResult()
            }
        }
    }
}

internal object EmptyArray : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf("kotlin.emptyArray", "kotlin.ArrayIntrinsicsKt.emptyArray")
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val returnType = environment.callStack.loadState(irFunction.symbol) as KTypeState
        environment.callStack.pushState(environment.convertToState(emptyArray<Any?>(), returnType.irType))
    }
}

internal object ArrayOf : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf(
            "kotlin.arrayOf", "kotlin.byteArrayOf", "kotlin.charArrayOf", "kotlin.shortArrayOf", "kotlin.intArrayOf",
            "kotlin.longArrayOf", "kotlin.floatArrayOf", "kotlin.doubleArrayOf", "kotlin.booleanArrayOf"
        )
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val elementsSymbol = irFunction.valueParameters.single().symbol
        val varargVariable = environment.callStack.loadState(elementsSymbol)
        environment.callStack.pushState(varargVariable)
    }
}

internal object ArrayOfNulls : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf("kotlin.arrayOfNulls")
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val size = environment.callStack.loadState(irFunction.valueParameters.first().symbol).asInt()
        val array = arrayOfNulls<Any?>(size)
        val typeArgument = irFunction.typeParameters.map { environment.callStack.loadState(it.symbol) }.single() as KTypeState
        val returnType = (irFunction.returnType as IrSimpleType).buildSimpleType {
            arguments = listOf(makeTypeProjection(typeArgument.irType, Variance.INVARIANT))
        }

        environment.callStack.pushState(environment.convertToState(array, returnType))
    }
}

internal object EnumValues : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf("kotlin.enumValues")
    }

    private fun getEnumClass(irFunction: IrFunction, environment: IrInterpreterEnvironment): IrClass {
        return when (irFunction.fqName) {
            "kotlin.enumValues" -> {
                val kType = environment.callStack.loadState(irFunction.typeParameters.first().symbol) as KTypeState
                kType.irType.classOrNull!!.owner
            }
            else -> irFunction.parent as IrClass
        }
    }

    override fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        val enumClass = getEnumClass(irFunction, environment)
        val enumEntries = enumClass.declarations.filterIsInstance<IrEnumEntry>()

        return super.unwind(irFunction, environment) + enumEntries.reversed().map { SimpleInstruction(it) }
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val enumClass = getEnumClass(irFunction, environment)

        val enumEntries = enumClass.declarations.filterIsInstance<IrEnumEntry>().map { environment.mapOfEnums[it.symbol] }
        environment.callStack.pushState(environment.convertToState(enumEntries.toTypedArray(), irFunction.returnType))
    }
}

internal object EnumValueOf : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf("kotlin.enumValueOf")
    }

    private fun getEnumClass(irFunction: IrFunction, environment: IrInterpreterEnvironment): IrClass {
        return when (irFunction.fqName) {
            "kotlin.enumValueOf" -> {
                val kType = environment.callStack.loadState(irFunction.typeParameters.first().symbol) as KTypeState
                kType.irType.classOrNull!!.owner
            }
            else -> irFunction.parent as IrClass
        }
    }

    private fun getEnumEntryByName(irFunction: IrFunction, environment: IrInterpreterEnvironment): IrEnumEntry? {
        val enumClass = getEnumClass(irFunction, environment)
        val enumEntryName = environment.callStack.loadState(irFunction.valueParameters.first().symbol).asString()
        val enumEntry = enumClass.declarations.filterIsInstance<IrEnumEntry>().singleOrNull { it.name.asString() == enumEntryName }
        if (enumEntry == null) {
            IllegalArgumentException("No enum constant ${enumClass.fqName}.$enumEntryName").handleUserException(environment)
        }
        return enumEntry
    }

    override fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        val enumEntry = getEnumEntryByName(irFunction, environment) ?: return emptyList()
        return super.unwind(irFunction, environment) + SimpleInstruction(enumEntry)
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val enumEntry = getEnumEntryByName(irFunction, environment)!!
        environment.callStack.pushState(environment.mapOfEnums[enumEntry.symbol]!!)
    }
}

internal object EnumIntrinsics : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        // functions that can be handled by this intrinsic cannot be described by single string
        // must call instead `canHandleFunctionWithName` method
        return listOf()
    }

    fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        if (origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER) return true
        return fqName.startsWith("kotlin.Enum.") && fqName != "kotlin.Enum.<init>"
    }

    override fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        return when (irFunction.name.asString()) {
            "values" -> EnumValues.unwind(irFunction, environment)
            "valueOf" -> EnumValueOf.unwind(irFunction, environment)
            else -> super.unwind(irFunction, environment)
        }
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val callStack = environment.callStack
        val enumEntry = callStack.loadState(irFunction.dispatchReceiverParameter!!.symbol)
        when (irFunction.name.asString()) {
            "<get-name>", "<get-ordinal>" -> {
                val symbol = irFunction.property!!.symbol
                callStack.pushState(enumEntry.getField(symbol)!!)
            }
            "compareTo" -> {
                val ordinalSymbol = enumEntry.irClass.getOriginalPropertyByName("ordinal").symbol
                val other = callStack.loadState(irFunction.valueParameters.single().symbol)
                val compareTo = enumEntry.getField(ordinalSymbol)!!.asInt().compareTo(other.getField(ordinalSymbol)!!.asInt())
                callStack.pushState(environment.convertToState(compareTo, irFunction.returnType))
            }
            // TODO "clone" -> throw exception
            "equals" -> {
                val other = callStack.loadState(irFunction.valueParameters.single().symbol)
                callStack.pushState(environment.convertToState((enumEntry === other), irFunction.returnType))
            }
            "hashCode" -> callStack.pushState(environment.convertToState(enumEntry.hashCode(), irFunction.returnType))
            "toString" -> {
                val nameSymbol = enumEntry.irClass.getOriginalPropertyByName("name").symbol
                callStack.pushState(enumEntry.getField(nameSymbol)!!)
            }
            "values" -> EnumValues.evaluate(irFunction, environment)
            "valueOf" -> EnumValueOf.evaluate(irFunction, environment)
        }
    }
}

internal object JsPrimitives : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf("kotlin.Long.<init>", "kotlin.Char.<init>")
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        when (irFunction.fqName) {
            "kotlin.Long.<init>" -> {
                val low = environment.callStack.loadState(irFunction.valueParameters[0].symbol).asInt()
                val high = environment.callStack.loadState(irFunction.valueParameters[1].symbol).asInt()
                environment.callStack.pushState(environment.convertToState((high.toLong().shl(32) + low), irFunction.returnType))
            }
            "kotlin.Char.<init>" -> {
                val value = environment.callStack.loadState(irFunction.valueParameters[0].symbol).asInt()
                environment.callStack.pushState(environment.convertToState(value.toChar(), irFunction.returnType))
            }
        }
    }
}

internal object ArrayConstructor : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf(
            "kotlin.Array.<init>",
            "kotlin.ByteArray.<init>", "kotlin.CharArray.<init>", "kotlin.ShortArray.<init>", "kotlin.IntArray.<init>",
            "kotlin.LongArray.<init>", "kotlin.FloatArray.<init>", "kotlin.DoubleArray.<init>", "kotlin.BooleanArray.<init>"
        )
    }

    override fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        if (irFunction.valueParameters.size == 1) return super.unwind(irFunction, environment)
        val callStack = environment.callStack
        val instructions = super.unwind(irFunction, environment).toMutableList()

        val sizeSymbol = irFunction.valueParameters[0].symbol
        val size = callStack.loadState(sizeSymbol).asInt()

        val initSymbol = irFunction.valueParameters[1].symbol
        val state = callStack.loadState(initSymbol).let {
            (it as? KFunctionState) ?: (it as KPropertyState).convertGetterToKFunctionState(environment)
        }
        // if property was converted, then we must replace symbol in memory to get correct receiver later
        callStack.rewriteState(initSymbol, state)

        for (i in size - 1 downTo 0) {
            val call = (state.invokeSymbol.owner as IrSimpleFunction).createCall()
            call.dispatchReceiver = initSymbol.owner.createGetValue()
            call.putValueArgument(0, i.toIrConst(environment.irBuiltIns.intType))
            instructions += CompoundInstruction(call)
        }

        return instructions
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val sizeDescriptor = irFunction.valueParameters[0].symbol
        val size = environment.callStack.loadState(sizeDescriptor).asInt()
        val arrayValue = MutableList<Any?>(size) {
            when {
                irFunction.returnType.isCharArray() -> 0.toChar()
                irFunction.returnType.isBooleanArray() -> false
                else -> 0
            }
        }

        if (irFunction.valueParameters.size == 2) {
            for (i in size - 1 downTo 0) {
                arrayValue[i] = environment.callStack.popState().let {
                    // TODO may be use wrap
                    when (it) {
                        is Wrapper -> it.value
                        is Primitive<*> -> if (it.type.isArray() || it.type.isPrimitiveArray()) it else it.value
                        else -> it
                    }
                }
            }
        }

        val type = (environment.callStack.loadState(irFunction.symbol) as KTypeState).irType
        environment.callStack.pushState(arrayValue.toPrimitiveStateArray(type))
    }
}

internal object SourceLocation : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf("kotlin.experimental.sourceLocation", "kotlin.experimental.SourceLocationKt.sourceLocation")
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        environment.callStack.pushState(environment.convertToState(environment.callStack.getFileAndPositionInfo(), irFunction.returnType))
    }
}

internal object AssertIntrinsic : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf("kotlin.PreconditionsKt.assert")
    }

    override fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        if (irFunction.valueParameters.size == 1) return super.unwind(irFunction, environment)

        val lambdaParameter = irFunction.valueParameters.last()
        val lambdaState = environment.callStack.loadState(lambdaParameter.symbol) as KFunctionState
        val call = (lambdaState.invokeSymbol.owner as IrSimpleFunction).createCall()
        call.dispatchReceiver = lambdaParameter.createGetValue()

        return super.unwind(irFunction, environment) + CompoundInstruction(call)
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val value = environment.callStack.loadState(irFunction.valueParameters.first().symbol).asBoolean()
        if (value) return
        when (irFunction.valueParameters.size) {
            1 -> AssertionError("Assertion failed").handleUserException(environment)
            2 -> AssertionError(environment.callStack.popState().asString()).handleUserException(environment)
        }
    }
}

internal object DataClassArrayToString : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf("kotlin.internal.ir.dataClassArrayMemberToString")
    }

    private fun arrayToString(array: Any?): String {
        return when (array) {
            null -> "null"
            is Array<*> -> Arrays.toString(array)
            is ByteArray -> Arrays.toString(array)
            is ShortArray -> Arrays.toString(array)
            is IntArray -> Arrays.toString(array)
            is LongArray -> Arrays.toString(array)
            is CharArray -> Arrays.toString(array)
            is BooleanArray -> Arrays.toString(array)
            is FloatArray -> Arrays.toString(array)
            is DoubleArray -> Arrays.toString(array)
            else -> stop { "Only arrays are supported in `dataClassArrayMemberToString` call" }
        }
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val array = environment.callStack.loadState(irFunction.valueParameters.single().symbol) as Primitive<*>
        environment.callStack.pushState(environment.convertToState(arrayToString(array.value), irFunction.returnType))
    }
}

internal object Indent : IntrinsicBase() {
    override fun getListOfAcceptableFunctions(): List<String> {
        return listOf(
            "kotlin.text.StringsKt.trimIndent", "kotlin.text.trimIndent",
            "kotlin.text.StringsKt.trimMargin", "kotlin.text.trimMargin",
            "kotlin.text.StringsKt.trimMargin\$default", "kotlin.text.trimMargin\$default",
        )
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val str = environment.callStack.loadState(irFunction.getExtensionReceiver()!!).asString()
        val trimmed = when (irFunction.fqName) {
            "kotlin.text.StringsKt.trimIndent", "kotlin.text.trimIndent" -> str.trimIndent()
            "kotlin.text.StringsKt.trimMargin", "kotlin.text.trimMargin" -> {
                val marginPrefix = environment.callStack.loadState(irFunction.valueParameters.single().symbol).asString()
                str.trimMargin(marginPrefix)
            }
            "kotlin.text.StringsKt.trimMargin\$default", "kotlin.text.trimMargin\$default" -> str.trimMargin()
            else -> TODO("unknown trim function")
        }
        environment.callStack.pushState(environment.convertToState(trimmed, irFunction.returnType))
    }
}
