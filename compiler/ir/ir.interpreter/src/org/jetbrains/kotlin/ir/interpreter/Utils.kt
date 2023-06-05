/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.interpreter.exceptions.handleUserException
import org.jetbrains.kotlin.ir.interpreter.proxy.wrap
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KTypeState
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.keysToMap
import java.lang.invoke.MethodType
import kotlin.math.floor

val intrinsicConstEvaluationAnnotation = FqName("kotlin.internal.IntrinsicConstEvaluation")
val compileTimeAnnotation = FqName("kotlin.CompileTimeCalculation")
val evaluateIntrinsicAnnotation = FqName("kotlin.EvaluateIntrinsic")

internal val IrElement.fqName: String
    get() = (this as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString() ?: ""

internal fun IrFunction.getDispatchReceiver(): IrValueParameterSymbol? = this.dispatchReceiverParameter?.symbol

internal fun IrFunction.getExtensionReceiver(): IrValueParameterSymbol? = this.extensionReceiverParameter?.symbol

internal fun IrFunction.getReceiver(): IrSymbol? = this.getDispatchReceiver() ?: this.getExtensionReceiver()

internal fun IrFunctionAccessExpression.getThisReceiver(): IrValueSymbol = this.symbol.owner.parentAsClass.thisReceiver!!.symbol

@Suppress("UNCHECKED_CAST")
internal fun <T> IrConst<T>.toPrimitive(): Primitive<T> = when {
    type.isByte() -> Primitive((value as Number).toByte() as T, type)
    type.isShort() -> Primitive((value as Number).toShort() as T, type)
    else -> Primitive(value, type)
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
    if (this is IrClass && this.fqName.startsWith("java")) return this.fqName
    if (!this.hasAnnotation(evaluateIntrinsicAnnotation)) return null
    return (this.getAnnotation(evaluateIntrinsicAnnotation).getValueArgument(0) as IrConst<*>).value.toString()
}

internal fun getPrimitiveClass(irType: IrType, asObject: Boolean = false): Class<*>? =
    when (irType.getPrimitiveType()) {
        PrimitiveType.BOOLEAN -> if (asObject) Boolean::class.javaObjectType else Boolean::class.java
        PrimitiveType.CHAR -> if (asObject) Char::class.javaObjectType else Char::class.java
        PrimitiveType.BYTE -> if (asObject) Byte::class.javaObjectType else Byte::class.java
        PrimitiveType.SHORT -> if (asObject) Short::class.javaObjectType else Short::class.java
        PrimitiveType.INT -> if (asObject) Int::class.javaObjectType else Int::class.java
        PrimitiveType.FLOAT -> if (asObject) Float::class.javaObjectType else Float::class.java
        PrimitiveType.LONG -> if (asObject) Long::class.javaObjectType else Long::class.java
        PrimitiveType.DOUBLE -> if (asObject) Double::class.javaObjectType else Double::class.java
        else -> when {
            irType.isString() -> String::class.java
            else -> null
        }
    }

fun IrFunction.getFirstNonInterfaceOverridden(): IrFunction {
    if (this !is IrSimpleFunction) return this

    return generateSequence(listOf(this)) {
        it.firstOrNull()?.overriddenSymbols?.map { overriddenSymbol -> overriddenSymbol.owner }
    }.flatten().first { overriddenFunction ->
        if (overriddenFunction.isFakeOverride) return@first false
        val kind = overriddenFunction.parentClassOrNull?.kind
        kind != ClassKind.INTERFACE
    }
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
        type.isBooleanArray() -> Primitive(BooleanArray(size) { i -> (this[i] as Boolean) }, type)
        else -> Primitive<Array<*>>(this.toTypedArray(), type)
    }
}

fun IrFunctionAccessExpression.getVarargType(index: Int): IrType? {
    val varargType = this.symbol.owner.valueParameters[index].varargElementType ?: return null
    varargType.classOrNull?.let { return this.symbol.owner.valueParameters[index].type }
    val type = this.symbol.owner.valueParameters[index].type as? IrSimpleType ?: return null
    return type.buildSimpleType {
        val typeParameter = varargType.classifierOrFail.owner as IrTypeParameter
        arguments = listOf(makeTypeProjection(this@getVarargType.getTypeArgument(typeParameter.index)!!, Variance.OUT_VARIANCE))
    }
}

internal fun IrFunction.getCapitalizedFileName(): String {
    return this.fileOrNull?.name?.replace(".kt", "Kt")?.capitalizeAsciiOnly() ?: "<UNKNOWN>"
}

internal fun IrClass.isSubclassOfThrowable(): Boolean {
    return generateSequence(this) { irClass ->
        if (irClass.defaultType.isAny()) return@generateSequence null
        irClass.superTypes.mapNotNull { it.classOrNull?.owner }.singleOrNull { it.isClass }
    }.any { it.defaultType.isThrowable() }
}

internal fun IrType.isUnsignedArray(): Boolean {
    if (this !is IrSimpleType || classifier !is IrClassSymbol) return false
    return classifier.owner.fqName in setOf("kotlin.UByteArray", "kotlin.UShortArray", "kotlin.UIntArray", "kotlin.ULongArray")
}

internal fun IrType.isPrimitiveArray(): Boolean {
    return this.getClass()?.fqNameWhenAvailable?.toUnsafe()?.let { StandardNames.isPrimitiveArray(it) } ?: false
}

internal fun IrClass.internalName(): String {
    val internalName = StringBuilder(this.name.asString())
    generateSequence(this as? IrDeclarationParent) { (it as? IrDeclaration)?.parent }
        .drop(1)
        .forEach {
            when (it) {
                is IrClass -> internalName.insert(0, it.name.asString() + "$")
                is IrPackageFragment -> it.packageFqName.asString().takeIf { it.isNotEmpty() }?.let { internalName.insert(0, "$it.") }
            }
        }
    return internalName.toString()
}

/**
 * This method is analog of `checkcast` jvm bytecode operation. Throw exception whenever actual type is not a subtype of expected.
 */
internal fun IrFunction?.checkCast(environment: IrInterpreterEnvironment): Boolean {
    this ?: return true
    val actualType = this.returnType
    if (actualType.classifierOrNull !is IrTypeParameterSymbol) return true

    // TODO expectedType can be missing for functions called as proxy
    val expectedType = (environment.callStack.loadState(this.symbol) as? KTypeState)?.irType ?: return true
    if (expectedType.classifierOrFail is IrTypeParameterSymbol) return true

    val actualState = environment.callStack.peekState() ?: return true
    if (actualState is Primitive<*> && actualState.value == null) return true // this is handled in checkNullability

    if (!actualState.isSubtypeOf(expectedType)) {
        val convertibleClassName = environment.callStack.popState().irClass.fqName
        environment.callStack.dropFrame() // current frame is pointing on function and is redundant
        ClassCastException("$convertibleClassName cannot be cast to ${expectedType.render()}").handleUserException(environment)
        return false
    }
    return true
}

internal fun IrFunction.getArgsForMethodInvocation(
    callInterceptor: CallInterceptor, methodType: MethodType, args: List<State>
): List<Any?> {
    val argsValues = args.wrap(callInterceptor, this, methodType).toMutableList()

    // TODO if vararg isn't last parameter
    // must convert vararg array into separated elements for correct invoke
    if (this.valueParameters.lastOrNull()?.varargElementType != null) {
        val varargValue = argsValues.last()
        argsValues.removeAt(argsValues.size - 1)
        argsValues.addAll(varargValue as Array<out Any?>)
    }

    return argsValues
}

internal fun IrType.fqNameWithNullability(): String {
    val fqName = classFqName?.toString()
        ?: (this.classifierOrNull?.owner as? IrDeclarationWithName)?.name?.asString()
        ?: render()
    val nullability = if (this is IrSimpleType && this.nullability == SimpleTypeNullability.MARKED_NULLABLE) "?" else ""
    return fqName + nullability
}

internal fun IrType.getOnlyName(): String {
    if (this !is IrSimpleType) return this.render()
    return (this.classifierOrFail.owner as IrDeclarationWithName).name.asString() + when (nullability) {
        SimpleTypeNullability.MARKED_NULLABLE -> "?"
        SimpleTypeNullability.NOT_SPECIFIED -> ""
        SimpleTypeNullability.DEFINITELY_NOT_NULL -> if (this.classifierOrNull is IrTypeParameterSymbol) " & Any" else ""
    }
}

internal fun IrFieldAccessExpression.accessesTopLevelOrObjectField(): Boolean {
    return this.receiver == null || (this.receiver?.type?.classifierOrNull?.owner as? IrClass)?.isObject == true
}

internal fun IrClass.getOriginalPropertyByName(name: String): IrProperty {
    val property = this.properties.single { it.name.asString() == name }
    return property.getter!!.getLastOverridden().property!!
}

internal fun IrFunctionAccessExpression.getFunctionThatContainsDefaults(): IrFunction {
    val irFunction = this.symbol.owner
    fun IrValueParameter.lookup(): IrFunction? {
        return defaultValue?.let { this.parent as IrFunction }
            ?: (this.parent as? IrSimpleFunction)?.overriddenSymbols
                ?.map { it.owner.valueParameters[this.index] }
                ?.firstNotNullOfOrNull { it.lookup() }
    }

    return (0 until this.valueArgumentsCount)
        .first { this.getValueArgument(it) == null }
        .let { irFunction.valueParameters[it].lookup() ?: irFunction }
}

internal fun IrValueParameter.getDefaultWithActualParameters(
    newParent: IrFunction, actualParameters: List<IrValueDeclaration?>
): IrExpression? {
    val expression = this.defaultValue?.expression
    if (expression is IrConst<*>) return expression

    val parameterOwner = this.parent as IrFunction
    val transformer = object : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val parameter = expression.symbol.owner as? IrValueParameter ?: return super.visitGetValue(expression)
            if (parameter.parent != parameterOwner) return super.visitGetValue(expression)
            val newParameter = when (parameter.index) {
                -1 -> newParent.dispatchReceiverParameter ?: newParent.extensionReceiverParameter
                else -> actualParameters[parameter.index]
            }
            return IrGetValueImpl(expression.startOffset, expression.endOffset, expression.type, newParameter!!.symbol)
        }
    }
    return expression?.deepCopyWithSymbols(newParent)?.transform(transformer, null)
}

internal fun IrType.getTypeIfReified(callStack: CallStack): IrType {
    return this.getTypeIfReified { (callStack.loadState(it) as KTypeState).irType }
}

internal fun IrType.getTypeIfReified(getType: (IrClassifierSymbol) -> IrType): IrType {
    if (this !is IrSimpleType) return this

    val owner = this.classifierOrNull?.owner
    if (owner is IrTypeParameter && owner.isReified) {
        return (getType(owner.symbol) as IrSimpleType)
            .mergeNullability(this)
    }

    val newArguments = this.arguments.map {
        val type = it.typeOrNull ?: return@map it
        val typeOwner = type.classifierOrNull?.owner
        if (typeOwner is IrTypeParameter && !typeOwner.isReified) return@map it
        type.getTypeIfReified(getType) as IrTypeArgument
    }
    return this.buildSimpleType {
        arguments = newArguments
    }
}

internal fun IrInterpreterEnvironment.loadReifiedTypeArguments(expression: IrFunctionAccessExpression): Map<IrTypeParameterSymbol, KTypeState> {
    return expression.symbol.owner.typeParameters.filter { it.isReified }.map { it.symbol }.keysToMap {
        val reifiedType = expression.getTypeArgument(it.owner.index)!!.getTypeIfReified(callStack)
        KTypeState(reifiedType, this.kTypeClass.owner)
    }
}

internal fun IrFunction.hasFunInterfaceParent(): Boolean {
    return this.parentClassOrNull?.isFun == true
}

internal fun IrClass.getSingleAbstractMethod(): IrFunction {
    return declarations.filterIsInstance<IrSimpleFunction>().single { it.modality == Modality.ABSTRACT }
}

internal fun IrExpression?.isAccessToNotNullableObject(): Boolean {
    if (this !is IrGetValue) return false
    val owner = this.symbol.owner
    val expectedClass = this.type.classOrNull?.owner
    if (expectedClass == null || !expectedClass.isObject || this.type.isNullable()) return false
    return owner.origin == IrDeclarationOrigin.INSTANCE_RECEIVER || owner.name.asString() == "<this>"
}

internal fun IrFunction.isAccessorOfPropertyWithBackingField(): Boolean {
    return this is IrSimpleFunction && this.correspondingPropertySymbol?.owner?.backingField?.initializer != null
}

internal fun State.unsignedToString(): String {
    return when (val value = (this.fields.values.single() as Primitive<*>).value) {
        is Byte -> value.toUByte().toString()
        is Short -> value.toUShort().toString()
        is Int -> value.toUInt().toString()
        else -> (value as Number).toLong().toULong().toString()
    }
}

internal fun Any?.specialToStringForJs(): String {
    return when {
        this is Float && !this.isInfinite() && floor(this) == this -> this.toInt().toString()
        this is Double && !this.isInfinite() && floor(this) == this -> this.toLong().toString()
        else -> this.toString()
    }
}

internal fun IrEnumEntry.toState(irBuiltIns: IrBuiltIns): Common {
    val enumClass = this.symbol.owner.parentAsClass
    val enumEntries = enumClass.declarations.filterIsInstance<IrEnumEntry>()
    val enumClassObject = Common(this.correspondingClass ?: enumClass)

    if (enumEntries.isNotEmpty()) {
        val valueArguments = listOf(
            Primitive(this.name.asString(), irBuiltIns.stringType),
            Primitive(enumEntries.indexOf(this), irBuiltIns.intType)
        )
        irBuiltIns.enumClass.owner.declarations.filterIsInstance<IrProperty>().zip(valueArguments).forEach { (property, argument) ->
            enumClassObject.setField(property.symbol, argument)
        }
    }

    return enumClassObject
}

internal val IrFunction.property: IrProperty?
    get() = (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner

