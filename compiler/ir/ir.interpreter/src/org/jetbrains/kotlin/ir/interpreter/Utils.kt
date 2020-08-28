/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.interpreter.builtins.evaluateIntrinsicAnnotation
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal fun IrFunction.getDispatchReceiver(): IrValueParameterSymbol? = this.dispatchReceiverParameter?.symbol

internal fun IrFunction.getExtensionReceiver(): IrValueParameterSymbol? = this.extensionReceiverParameter?.symbol

internal fun IrFunction.getReceiver(): IrSymbol? = this.getDispatchReceiver() ?: this.getExtensionReceiver()

internal fun IrFunctionAccessExpression.getBody(): IrBody? = this.symbol.owner.body

internal fun State.toIrExpression(expression: IrExpression): IrExpression {
    val start = expression.startOffset
    val end = expression.endOffset
    val type = expression.type.makeNotNull()
    return when (this) {
        is Primitive<*> ->
            when {
                this.value == null -> this.value.toIrConst(type, start, end)
                type.isPrimitiveType() || type.isString() -> this.value.toIrConst(type, start, end)
                else -> expression // TODO support for arrays
            }
        is Complex -> {
            val stateType = this.irClass.defaultType
            when {
                stateType.isUnsigned() -> (this.fields.single().state as Primitive<*>).value.toIrConst(type, start, end)
                else -> expression
            }
        }
        else -> expression // TODO support
    }
}

internal fun Any?.toState(irType: IrType): State {
    return when (this) {
        is State -> this
        is Boolean, is Char, is Byte, is Short, is Int, is Long, is String, is Float, is Double, is Array<*>, is ByteArray,
        is CharArray, is ShortArray, is IntArray, is LongArray, is FloatArray, is DoubleArray, is BooleanArray -> Primitive(this, irType)
        null -> Primitive(this, irType)
        else -> Wrapper(this, irType.classOrNull!!.owner)
    }
}

fun Any?.toIrConst(irType: IrType, startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET): IrConst<*> {
    val constType = irType.makeNotNull()
    return when {
        this == null -> IrConstImpl.constNull(startOffset, endOffset, irType)
        constType.isBoolean() -> IrConstImpl.boolean(startOffset, endOffset, constType, this as Boolean)
        constType.isChar() -> IrConstImpl.char(startOffset, endOffset, constType, this as Char)
        constType.isByte() -> IrConstImpl.byte(startOffset, endOffset, constType, (this as Number).toByte())
        constType.isShort() -> IrConstImpl.short(startOffset, endOffset, constType, (this as Number).toShort())
        constType.isInt() -> IrConstImpl.int(startOffset, endOffset, constType, (this as Number).toInt())
        constType.isLong() -> IrConstImpl.long(startOffset, endOffset, constType, (this as Number).toLong())
        constType.isString() -> IrConstImpl.string(startOffset, endOffset, constType, this as String)
        constType.isFloat() -> IrConstImpl.float(startOffset, endOffset, constType, (this as Number).toFloat())
        constType.isDouble() -> IrConstImpl.double(startOffset, endOffset, constType, (this as Number).toDouble())
        constType.isUByte() -> IrConstImpl.byte(startOffset, endOffset, constType, (this as Number).toByte())
        constType.isUShort() -> IrConstImpl.short(startOffset, endOffset, constType, (this as Number).toShort())
        constType.isUInt() -> IrConstImpl.int(startOffset, endOffset, constType, (this as Number).toInt())
        constType.isULong() -> IrConstImpl.long(startOffset, endOffset, constType, (this as Number).toLong())
        else -> throw UnsupportedOperationException("Unsupported const element type ${constType.render()}")
    }
}

internal fun <T> IrConst<T>.toPrimitive(): Primitive<T> {
    return Primitive(this.value, this.type)
}

fun IrAnnotationContainer?.hasAnnotation(annotation: FqName): Boolean {
    this ?: return false
    if (this.annotations.isNotEmpty()) {
        return this.annotations.any { it.symbol.owner.parentAsClass.fqNameWhenAvailable == annotation }
    }
    return false
}

fun IrAnnotationContainer.getAnnotation(annotation: FqName): IrConstructorCall {
    return this.annotations.firstOrNull { it.symbol.owner.parentAsClass.fqNameWhenAvailable == annotation }
        ?: ((this as IrFunction).parent as IrClass).annotations.first { it.symbol.owner.parentAsClass.fqNameWhenAvailable == annotation }
}

internal fun IrAnnotationContainer.getEvaluateIntrinsicValue(): String? {
    if (this is IrClass && this.fqNameWhenAvailable?.startsWith(Name.identifier("java")) == true) return this.fqNameWhenAvailable?.asString()
    if (!this.hasAnnotation(evaluateIntrinsicAnnotation)) return null
    return (this.getAnnotation(evaluateIntrinsicAnnotation).getValueArgument(0) as IrConst<*>).value.toString()
}

internal fun getPrimitiveClass(irType: IrType, asObject: Boolean = false): Class<*>? {
    return when {
        irType.isBoolean() -> if (asObject) Boolean::class.javaObjectType else Boolean::class.java
        irType.isChar() -> if (asObject) Char::class.javaObjectType else Char::class.java
        irType.isByte() -> if (asObject) Byte::class.javaObjectType else Byte::class.java
        irType.isShort() -> if (asObject) Short::class.javaObjectType else Short::class.java
        irType.isInt() -> if (asObject) Int::class.javaObjectType else Int::class.java
        irType.isLong() -> if (asObject) Long::class.javaObjectType else Long::class.java
        irType.isString() -> if (asObject) String::class.javaObjectType else String::class.java
        irType.isFloat() -> if (asObject) Float::class.javaObjectType else Float::class.java
        irType.isDouble() -> if (asObject) Double::class.javaObjectType else Double::class.java
        else -> null
    }
}

internal fun IrFunction.getArgsForMethodInvocation(args: List<Variable>): List<Any?> {
    val argsValues = args.map {
        when (val state = it.state) {
            is ExceptionState -> state.getThisAsCauseForException()
            is Wrapper -> state.value
            is Primitive<*> -> state.value
            else -> throw AssertionError("${state::class} is unsupported as argument for wrapper method invocation")
        }
    }.toMutableList()

    // TODO if vararg isn't last parameter
    // must convert vararg array into separated elements for correct invoke
    if (this.valueParameters.lastOrNull()?.varargElementType != null) {
        val varargValue = argsValues.last()
        argsValues.removeAt(argsValues.size - 1)
        argsValues.addAll(varargValue as Array<out Any?>)
    }

    return argsValues
}

fun IrFunction.getLastOverridden(): IrFunction {
    if (this !is IrSimpleFunction) return this

    return generateSequence(listOf(this)) { it.firstOrNull()?.overriddenSymbols?.map { it.owner } }.flatten().last()
}

internal fun List<Any?>.toPrimitiveStateArray(type: IrType): Primitive<*> {
    return when {
        type.isByteArray() -> Primitive(ByteArray(size) { i -> (this[i] as Number).toByte() }, type)
        type.isCharArray() -> Primitive(CharArray(size) { i -> this[i] as Char }, type)
        type.isShortArray() -> Primitive(ShortArray(size) { i -> (this[i] as Number).toShort() }, type)
        type.isIntArray() -> Primitive(IntArray(size) { i -> (this[i] as Number).toInt() }, type)
        type.isLongArray() -> Primitive(LongArray(size) { i -> (this[i] as Number).toLong() }, type)
        type.isFloatArray() -> Primitive(FloatArray(size) { i -> (this[i] as Number).toFloat() }, type)
        type.isDoubleArray() -> Primitive(DoubleArray(size) { i -> (this[i] as Number).toDouble() }, type)
        type.isBooleanArray() -> Primitive(BooleanArray(size) { i -> this[i].toString().toBoolean() }, type)
        else -> Primitive<Array<*>>(this.toTypedArray(), type)
    }
}

fun IrFunctionAccessExpression.getVarargType(index: Int): IrType? {
    val varargType = this.symbol.owner.valueParameters[index].varargElementType ?: return null
    varargType.classOrNull?.let { return this.symbol.owner.valueParameters[index].type }
    val typeParameter = varargType.classifierOrFail.owner as IrTypeParameter
    return this.getTypeArgument(typeParameter.index)
}

internal fun getTypeArguments(
    container: IrTypeParametersContainer, expression: IrFunctionAccessExpression, mapper: (IrTypeParameterSymbol) -> State
): List<Variable> {
    fun IrType.getState(): State {
        return this.classOrNull?.owner?.let { Common(it) } ?: mapper(this.classifierOrFail as IrTypeParameterSymbol)
    }

    val typeArguments = container.typeParameters.mapIndexed { index, typeParameter ->
        val typeArgument = expression.getTypeArgument(index)!!
        Variable(typeParameter.symbol, typeArgument.getState())
    }.toMutableList()

    if (container is IrSimpleFunction) {
        container.returnType.classifierOrFail.owner.safeAs<IrTypeParameter>()
            ?.let { typeArguments.add(Variable(it.symbol, expression.type.getState())) }
    }

    return typeArguments
}

internal fun State?.extractNonLocalDeclarations(): List<Variable> {
    this ?: return listOf()
    val state = this.takeIf { it !is Complex } ?: (this as Complex).getOriginal()
    return state.fields.filter { it.symbol !is IrFieldSymbol }
}

internal fun State?.getCorrectReceiverByFunction(irFunction: IrFunction): State? {
    if (this !is Complex) return this

    val original: Complex? = this.getOriginal()
    val other = irFunction.parentClassOrNull?.thisReceiver ?: return this
    return generateSequence(original) { it.superClass }.firstOrNull { it.irClass.thisReceiver == other } ?: this
}

internal fun IrFunction.getCapitalizedFileName() = this.file.name.replace(".kt", "Kt").capitalize()

internal fun IrType.isUnsigned() = this.isUByte() || this.isUShort() || this.isUInt() || this.isULong()

internal fun IrType.isPrimitiveArray(): Boolean {
    return this.getClass()?.fqNameWhenAvailable?.toUnsafe()?.let { StandardNames.isPrimitiveArray(it) } ?: false
}

internal fun IrType.isFunction() = this.getClass()?.fqNameWhenAvailable?.asString()?.startsWith("kotlin.Function") ?: false

internal fun IrType.isTypeParameter() = classifierOrNull is IrTypeParameterSymbol

internal fun IrType.isInterface() = classOrNull?.owner?.kind == ClassKind.INTERFACE

internal fun IrType.isThrowable() = this.getClass()?.fqNameWhenAvailable?.asString() == "kotlin.Throwable"

fun IrClass.internalName(): String {
    val internalName = StringBuilder(this.name.asString())
    generateSequence(this as? IrDeclarationParent) { (it as? IrDeclaration)?.parent }
        .drop(1)
        .forEach {
            when (it) {
                is IrClass -> internalName.insert(0, it.name.asString() + "$")
                is IrPackageFragment -> it.fqName.asString().takeIf { it.isNotEmpty() }?.let { internalName.insert(0, "$it.") }
            }
        }
    return internalName.toString()
}
