/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.intrinsics

import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.interpreter.stack.Stack
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.interpreter.exceptions.throwAsUserException
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isCharArray
import org.jetbrains.kotlin.ir.util.*

internal sealed class IntrinsicBase {
    abstract fun equalTo(irFunction: IrFunction): Boolean
    abstract fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult
}

internal object EmptyArray : IntrinsicBase() {
    override fun equalTo(irFunction: IrFunction): Boolean {
        val fqName = irFunction.fqNameWhenAvailable.toString()
        return fqName in setOf("kotlin.emptyArray", "kotlin.ArrayIntrinsicsKt.emptyArray")
    }

    override fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        val typeArguments = irFunction.typeParameters.map { stack.getVariable(it.symbol) }
        stack.pushReturnValue(emptyArray<Any?>().toState(irFunction.returnType).apply { addTypeArguments(typeArguments) })
        return Next
    }
}

internal object ArrayOf : IntrinsicBase() {
    override fun equalTo(irFunction: IrFunction): Boolean {
        val fqName = irFunction.fqNameWhenAvailable.toString()
        return fqName == "kotlin.arrayOf"
    }

    override fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        val elementsVariable = irFunction.valueParameters.single().symbol
        val array = (stack.getVariable(elementsVariable).state as Primitive<*>).value as Array<out Any?>
        val typeArguments = irFunction.typeParameters.map { stack.getVariable(it.symbol) }
        stack.pushReturnValue(array.toState(irFunction.returnType).apply { addTypeArguments(typeArguments) })
        return Next
    }
}

internal object ArrayOfNulls : IntrinsicBase() {
    override fun equalTo(irFunction: IrFunction): Boolean {
        val fqName = irFunction.fqNameWhenAvailable.toString()
        return fqName == "kotlin.arrayOfNulls"
    }

    override fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        val size = stack.getVariable(irFunction.valueParameters.first().symbol).state.asInt()
        val array = arrayOfNulls<Any?>(size)
        val typeArguments = irFunction.typeParameters.map { stack.getVariable(it.symbol) }
        stack.pushReturnValue(array.toState(irFunction.returnType).apply { addTypeArguments(typeArguments) })
        return Next
    }
}

internal object EnumValues : IntrinsicBase() {
    override fun equalTo(irFunction: IrFunction): Boolean {
        val fqName = irFunction.fqNameWhenAvailable.toString()
        return (fqName == "kotlin.enumValues" || fqName.endsWith(".values")) && irFunction.valueParameters.isEmpty()
    }

    override fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        val enumClass = when (irFunction.fqNameWhenAvailable.toString()) {
            "kotlin.enumValues" -> stack.getVariable(irFunction.typeParameters.first().symbol).state.irClass
            else -> irFunction.parent as IrClass
        }

        val enumEntries = enumClass.declarations.filterIsInstance<IrEnumEntry>()
            .map { entry -> entry.interpret().check { return it }.let { stack.popReturnValue() as Common } }
        stack.pushReturnValue(enumEntries.toTypedArray().toState(irFunction.returnType))
        return Next
    }
}

internal object EnumValueOf : IntrinsicBase() {
    override fun equalTo(irFunction: IrFunction): Boolean {
        val fqName = irFunction.fqNameWhenAvailable.toString()
        return (fqName == "kotlin.enumValueOf" || fqName.endsWith(".valueOf")) && irFunction.valueParameters.size == 1
    }

    override fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        val enumClass = when (irFunction.fqNameWhenAvailable.toString()) {
            "kotlin.enumValueOf" -> stack.getVariable(irFunction.typeParameters.first().symbol).state.irClass
            else -> irFunction.parent as IrClass
        }
        val enumEntryName = stack.getVariable(irFunction.valueParameters.first().symbol).state.asString()
        val enumEntry = enumClass.declarations.filterIsInstance<IrEnumEntry>().singleOrNull { it.name.asString() == enumEntryName }
        enumEntry?.interpret()?.check { return it }
            ?: IllegalArgumentException("No enum constant ${enumClass.fqNameWhenAvailable}.$enumEntryName").throwAsUserException()

        return Next
    }
}

internal object EnumHashCode : IntrinsicBase() {
    override fun equalTo(irFunction: IrFunction): Boolean {
        val fqName = irFunction.fqNameWhenAvailable.toString()
        return fqName == "kotlin.Enum.hashCode"
    }

    override fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        val hashCode = stack.getAll().single().state.hashCode()
        stack.pushReturnValue(hashCode.toState(irFunction.returnType))
        return Next
    }
}

internal object JsPrimitives : IntrinsicBase() {
    override fun equalTo(irFunction: IrFunction): Boolean {
        val fqName = irFunction.fqNameWhenAvailable.toString()
        return fqName == "kotlin.Long.<init>" || fqName == "kotlin.Char.<init>"
    }

    override fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        when (irFunction.fqNameWhenAvailable.toString()) {
            "kotlin.Long.<init>" -> {
                val low = stack.getVariable(irFunction.valueParameters[0].symbol).state.asInt()
                val high = stack.getVariable(irFunction.valueParameters[1].symbol).state.asInt()
                stack.pushReturnValue((high.toLong().shl(32) + low).toState(irFunction.returnType))
            }
            "kotlin.Char.<init>" -> {
                val value = stack.getVariable(irFunction.valueParameters[0].symbol).state.asInt()
                stack.pushReturnValue(value.toChar().toState(irFunction.returnType))
            }
        }
        return Next
    }
}

internal object ArrayConstructor : IntrinsicBase() {
    override fun equalTo(irFunction: IrFunction): Boolean {
        val fqName = irFunction.fqNameWhenAvailable.toString()
        return fqName.matches("kotlin\\.(Byte|Char|Short|Int|Long|Float|Double|Boolean|)Array\\.<init>".toRegex())
    }

    override fun evaluate(irFunction: IrFunction, stack: Stack, interpret: IrElement.() -> ExecutionResult): ExecutionResult {
        val sizeDescriptor = irFunction.valueParameters[0].symbol
        val size = stack.getVariable(sizeDescriptor).state.asInt()
        val arrayValue = MutableList<Any>(size) { if (irFunction.returnType.isCharArray()) 0.toChar() else 0 }

        if (irFunction.valueParameters.size == 2) {
            val initDescriptor = irFunction.valueParameters[1].symbol
            val initLambda = stack.getVariable(initDescriptor).state as Lambda
            val index = initLambda.irFunction.valueParameters.single()
            val nonLocalDeclarations = initLambda.extractNonLocalDeclarations()
            for (i in 0 until size) {
                val indexVar = listOf(Variable(index.symbol, i.toState(index.type)))
                // TODO throw exception if label != RETURN
                stack.newFrame(
                    asSubFrame = initLambda.irFunction.isLocal || initLambda.irFunction.isInline,
                    initPool = nonLocalDeclarations + indexVar
                ) { initLambda.irFunction.body!!.interpret() }.check(ReturnLabel.RETURN) { return it }
                arrayValue[i] = stack.popReturnValue().let { (it as? Wrapper)?.value ?: (it as? Primitive<*>)?.value ?: it }
            }
        }

        stack.pushReturnValue(arrayValue.toPrimitiveStateArray(irFunction.parentAsClass.defaultType))
        return Next
    }
}