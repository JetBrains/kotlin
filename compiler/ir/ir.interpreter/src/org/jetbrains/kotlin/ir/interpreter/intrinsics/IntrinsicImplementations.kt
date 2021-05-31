/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.intrinsics

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.interpreter.exceptions.handleUserException
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KFunctionState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KTypeState
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.types.Variance

internal sealed class IntrinsicBase {
    abstract fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean
    abstract fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment)
    open fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        return listOf(customEvaluateInstruction(irFunction, environment))
    }

    fun customEvaluateInstruction(irFunction: IrFunction, environment: IrInterpreterEnvironment): CustomInstruction {
        return CustomInstruction {
            evaluate(irFunction, environment)
            environment.callStack.dropFrameAndCopyResult()
        }
    }
}

internal object EmptyArray : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        return fqName in setOf("kotlin.emptyArray", "kotlin.ArrayIntrinsicsKt.emptyArray")
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val returnType = environment.callStack.getState(irFunction.symbol) as KTypeState
        environment.callStack.pushState(emptyArray<Any?>().toState(returnType.irType))
    }
}

internal object ArrayOf : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        return fqName == "kotlin.arrayOf" || fqName.matches("kotlin\\.(byte|char|short|int|long|float|double|boolean|)ArrayOf".toRegex())
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val elementsSymbol = irFunction.valueParameters.single().symbol
        val varargVariable = environment.callStack.getState(elementsSymbol)
        environment.callStack.pushState(varargVariable)
    }
}

internal object ArrayOfNulls : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        return fqName == "kotlin.arrayOfNulls"
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val size = environment.callStack.getState(irFunction.valueParameters.first().symbol).asInt()
        val array = arrayOfNulls<Any?>(size)
        val typeArgument = irFunction.typeParameters.map { environment.callStack.getState(it.symbol) }.single() as KTypeState
        val returnType = (irFunction.returnType as IrSimpleType).buildSimpleType {
            arguments = listOf(makeTypeProjection(typeArgument.irType, Variance.INVARIANT))
        }

        environment.callStack.pushState(array.toState(returnType))
    }
}

internal object EnumValues : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        return fqName == "kotlin.enumValues"
    }

    private fun getEnumClass(irFunction: IrFunction, environment: IrInterpreterEnvironment): IrClass {
        return when (irFunction.fqNameWhenAvailable.toString()) {
            "kotlin.enumValues" -> {
                val kType = environment.callStack.getState(irFunction.typeParameters.first().symbol) as KTypeState
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
        environment.callStack.pushState(enumEntries.toTypedArray().toState(irFunction.returnType))
    }
}

internal object EnumValueOf : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        return fqName == "kotlin.enumValueOf"
    }

    private fun getEnumClass(irFunction: IrFunction, environment: IrInterpreterEnvironment): IrClass {
        return when (irFunction.fqNameWhenAvailable.toString()) {
            "kotlin.enumValueOf" -> {
                val kType = environment.callStack.getState(irFunction.typeParameters.first().symbol) as KTypeState
                kType.irType.classOrNull!!.owner
            }
            else -> irFunction.parent as IrClass
        }
    }

    private fun getEnumEntryByName(irFunction: IrFunction, environment: IrInterpreterEnvironment): IrEnumEntry? {
        val enumClass = getEnumClass(irFunction, environment)
        val enumEntryName = environment.callStack.getState(irFunction.valueParameters.first().symbol).asString()
        val enumEntry = enumClass.declarations.filterIsInstance<IrEnumEntry>().singleOrNull { it.name.asString() == enumEntryName }
        if (enumEntry == null) {
            IllegalArgumentException("No enum constant ${enumClass.fqNameWhenAvailable}.$enumEntryName").handleUserException(environment)
        }
        return enumEntry
    }

    override fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        val enumEntry = getEnumEntryByName(irFunction, environment) ?: return emptyList()
        return listOf(customEvaluateInstruction(irFunction, environment), SimpleInstruction(enumEntry))
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val enumEntry = getEnumEntryByName(irFunction, environment)!!
        environment.callStack.pushState(environment.mapOfEnums[enumEntry.symbol]!!)
    }
}

internal object EnumIntrinsics : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        if (origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER) return true
        return fqName.startsWith("kotlin.Enum.")
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
        val enumEntry = callStack.getState(irFunction.dispatchReceiverParameter!!.symbol)
        when (irFunction.name.asString()) {
            "<get-name>", "<get-ordinal>" -> {
                val symbol = (irFunction as IrSimpleFunction).correspondingPropertySymbol!!
                callStack.pushState(enumEntry.getField(symbol)!!)
            }
            "compareTo" -> {
                val ordinal = enumEntry.irClass.declarations.filterIsInstance<IrProperty>()
                    .first { it.name.asString() == "ordinal" }
                    .resolveFakeOverride()!!
                val other = callStack.getState(irFunction.valueParameters.single().symbol)
                val compareTo = enumEntry.getField(ordinal.symbol)!!.asInt().compareTo(other.getField(ordinal.symbol)!!.asInt())
                callStack.pushState(compareTo.toState(irFunction.returnType))
            }
            // TODO "clone" -> throw exception
            "equals" -> {
                val other = callStack.getState(irFunction.valueParameters.single().symbol)
                callStack.pushState((enumEntry === other).toState(irFunction.returnType))
            }
            "hashCode" -> callStack.pushState(enumEntry.hashCode().toState(irFunction.returnType))
            "toString" -> {
                val name = enumEntry.irClass.declarations.filterIsInstance<IrProperty>()
                    .first { it.name.asString() == "name" }
                    .resolveFakeOverride()!!
                callStack.pushState(enumEntry.getField(name.symbol)!!)
            }
            "values" -> EnumValues.evaluate(irFunction, environment)
            "valueOf" -> EnumValueOf.evaluate(irFunction, environment)
        }
    }
}

internal object JsPrimitives : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        return fqName == "kotlin.Long.<init>" || fqName == "kotlin.Char.<init>"
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        when (irFunction.fqNameWhenAvailable.toString()) {
            "kotlin.Long.<init>" -> {
                val low = environment.callStack.getState(irFunction.valueParameters[0].symbol).asInt()
                val high = environment.callStack.getState(irFunction.valueParameters[1].symbol).asInt()
                environment.callStack.pushState((high.toLong().shl(32) + low).toState(irFunction.returnType))
            }
            "kotlin.Char.<init>" -> {
                val value = environment.callStack.getState(irFunction.valueParameters[0].symbol).asInt()
                environment.callStack.pushState(value.toChar().toState(irFunction.returnType))
            }
        }
    }
}

internal object ArrayConstructor : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        return fqName.matches("kotlin\\.(Byte|Char|Short|Int|Long|Float|Double|Boolean|)Array\\.<init>".toRegex())
    }

    override fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        if (irFunction.valueParameters.size == 1) return listOf(customEvaluateInstruction(irFunction, environment))
        val instructions = mutableListOf<Instruction>(customEvaluateInstruction(irFunction, environment))

        val sizeDescriptor = irFunction.valueParameters[0].symbol
        val size = environment.callStack.getState(sizeDescriptor).asInt()

        val initDescriptor = irFunction.valueParameters[1].symbol
        val initLambda = environment.callStack.getState(initDescriptor) as KFunctionState
        environment.callStack.loadUpValues(initLambda)
        val function = initLambda.irFunction as IrSimpleFunction
        val index = initLambda.irFunction.valueParameters.single()
        for (i in size - 1 downTo 0) {
            val call = IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, function.returnType, function.symbol)
            call.putValueArgument(0, IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, index.type, i))
            instructions += CompoundInstruction(call)
        }

        return instructions
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val sizeDescriptor = irFunction.valueParameters[0].symbol
        val size = environment.callStack.getState(sizeDescriptor).asInt()
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

        val type = environment.callStack.getState(irFunction.symbol) as KTypeState
        environment.callStack.pushState(arrayValue.toPrimitiveStateArray(type.irType))
    }
}

internal object SourceLocation : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        return fqName == "kotlin.experimental.sourceLocation" || fqName == "kotlin.experimental.SourceLocationKt.sourceLocation"
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        environment.callStack.pushState(environment.callStack.getFileAndPositionInfo().toState(irFunction.returnType))
    }
}

internal object AssertIntrinsic : IntrinsicBase() {
    override fun canHandleFunctionWithName(fqName: String, origin: IrDeclarationOrigin): Boolean {
        return fqName == "kotlin.PreconditionsKt.assert"
    }

    override fun unwind(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction> {
        if (irFunction.valueParameters.size == 1) return listOf(customEvaluateInstruction(irFunction, environment))

        val messageLambda = environment.callStack.getState(irFunction.valueParameters.last().symbol) as KFunctionState
        val function = messageLambda.irFunction as IrSimpleFunction
        val call = IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, function.returnType, function.symbol)

        return listOf(customEvaluateInstruction(irFunction, environment), CompoundInstruction(call))
    }

    override fun evaluate(irFunction: IrFunction, environment: IrInterpreterEnvironment) {
        val value = environment.callStack.getState(irFunction.valueParameters.first().symbol).asBoolean()
        if (value) return
        when (irFunction.valueParameters.size) {
            1 -> AssertionError("Assertion failed").handleUserException(environment)
            2 -> AssertionError(environment.callStack.popState().asString()).handleUserException(environment)
        }
    }
}