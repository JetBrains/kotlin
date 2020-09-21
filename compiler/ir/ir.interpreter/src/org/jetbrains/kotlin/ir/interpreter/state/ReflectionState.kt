/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.getLastOverridden
import org.jetbrains.kotlin.ir.interpreter.internalName
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KFunctionProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KProperty1Proxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import org.jetbrains.kotlin.ir.interpreter.renderType
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import kotlin.reflect.*

internal abstract class ReflectionState(val irClassifierSymbol: IrClassifierSymbol) : State {
    override val irClass: IrClass = irClassifierSymbol.extractClass()
    override val fields: MutableList<Variable> = mutableListOf()
    override val typeArguments: MutableList<Variable> = mutableListOf()

    constructor(irTypeParameter: IrTypeParameter) : this(irTypeParameter.superTypes.firstNotNullResult { it.classifierOrNull }!!)

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? = null

    protected fun renderReceivers(dispatchReceiver: IrType?, extensionReceiver: IrType?): String {
        return buildString {
            if (dispatchReceiver != null) {
                append(dispatchReceiver.renderType())
                append(".")
            }
            val addParentheses = dispatchReceiver != null && extensionReceiver != null
            if (addParentheses) append("(")
            if (extensionReceiver != null) {
                append(extensionReceiver.renderType())
                append(".")
            }
            if (addParentheses) append(")")
        }
    }

    companion object {
        private fun IrClassifierSymbol.extractClass(): IrClass {
            return (owner as? IrClass) ?: (owner as IrTypeParameter).extractAnyClass()
        }

        private fun IrTypeParameter.extractAnyClass(): IrClass {
            return this.superTypes
                .firstNotNullResult { it.classOrNull?.owner ?: (it.classifierOrNull?.owner as? IrTypeParameter)?.extractAnyClass() }!!
        }
    }
}

internal class KClassState(override val irClass: IrClass) : ReflectionState(irClass.symbol) {
    private var _members: Collection<KCallable<*>>? = null
    private var _constructors: Collection<KFunction<Proxy>>? = null
    private var _typeParameters: List<KTypeParameter>? = null
    private var _supertypes: List<KType>? = null

    constructor(classReference: IrClassReference) : this(classReference.symbol.owner as IrClass)

    fun getMembers(interpreter: IrInterpreter): Collection<KCallable<*>> {
        if (_members != null) return _members!!
        _members = irClass.declarations
            .filter { it !is IrClass && it !is IrConstructor }
            .map {
                when (it) {
                    is IrProperty -> KProperty1Proxy(KPropertyState(it, null, null), interpreter) // TODO KProperty2
                    is IrFunction -> KFunctionProxy(KFunctionState(it, interpreter.irBuiltIns.functionFactory), interpreter)
                    else -> TODO()
                }
            }
        return _members!!
    }

    fun getConstructors(interpreter: IrInterpreter): Collection<KFunction<Proxy>> {
        if (_constructors != null) return _constructors!!
        _constructors = irClass.declarations
            .filterIsInstance<IrConstructor>()
            .map { KFunctionProxy(KFunctionState(it, interpreter.irBuiltIns.functionFactory), interpreter) as KFunction<Proxy> }
        return _constructors!!
    }

    fun getTypeParameters(interpreter: IrInterpreter): List<KTypeParameter> {
        if (_typeParameters != null) return _typeParameters!!
        _typeParameters = irClass.typeParameters.map { KTypeParameterProxy(KTypeParameterState(it), interpreter) }
        return _typeParameters!!
    }

    fun getSupertypes(interpreter: IrInterpreter): List<KType> {
        if (_supertypes != null) return _supertypes!!
        _supertypes = (irClass.superTypes.map { it } + interpreter.irBuiltIns.anyType).toSet()
            .map { KTypeProxy(KTypeState(it), interpreter) }
        return _supertypes!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KClassState

        if (irClass != other.irClass) return false

        return true
    }

    override fun hashCode(): Int {
        return irClass.hashCode()
    }

    override fun toString(): String {
        return "class ${irClass.internalName()}"
    }
}

internal class KPropertyState(
    val property: IrProperty, val dispatchReceiver: State?, val extensionReceiver: State?
) : ReflectionState(property.parentAsClass.symbol) {
    constructor(propertyReference: IrPropertyReference, dispatchReceiver: State?, extensionReceiver: State?)
            : this(propertyReference.symbol.owner, dispatchReceiver, extensionReceiver)

    fun isKProperty0(): Boolean {
        return dispatchReceiver != null && extensionReceiver == null
    }

    fun isKProperty1(): Boolean {
        return dispatchReceiver == null && extensionReceiver == null
    }

    fun isKProperty2(): Boolean {
        return dispatchReceiver != null && extensionReceiver != null
    }

    fun isKMutableProperty0(): Boolean {
        return isKProperty0() && property.isVar
    }

    fun isKMutableProperty1(): Boolean {
        return isKProperty1() && property.isVar
    }

    fun isKMutableProperty2(): Boolean {
        return isKProperty1() && property.isVar
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KPropertyState

        if (property != other.property) return false
        if (dispatchReceiver != other.dispatchReceiver) return false
        if (extensionReceiver != other.extensionReceiver) return false

        return true
    }

    override fun hashCode(): Int {
        var result = property.hashCode()
        result = 31 * result + (dispatchReceiver?.hashCode() ?: 0)
        result = 31 * result + (extensionReceiver?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        val prefix = if (property.isVar) "var" else "val"
        val receivers = renderReceivers(property.getter?.dispatchReceiverParameter?.type, property.getter?.extensionReceiverParameter?.type)
        val returnType = property.getter!!.returnType.renderType()
        return "$prefix $receivers${property.name}: $returnType"
    }
}

internal class KFunctionState(val irFunction: IrFunction, override val irClass: IrClass) : ReflectionState(irClass.symbol) {
    override val fields: MutableList<Variable> = mutableListOf()
    override val typeArguments: MutableList<Variable> = mutableListOf()
    private var _typeParameters: List<KTypeParameter>? = null

    constructor(functionReference: IrFunctionReference) : this(functionReference.symbol.owner, functionReference.type.classOrNull!!.owner)
    constructor(irFunction: IrFunction, functionFactory: IrAbstractFunctionFactory) :
            this(irFunction, functionFactory.kFunctionN(irFunction.valueParameters.size))

    fun getTypeParameters(interpreter: IrInterpreter): List<KTypeParameter> {
        if (_typeParameters != null) return _typeParameters!!
        _typeParameters = irClass.typeParameters.map { KTypeParameterProxy(KTypeParameterState(it), interpreter) }
        return _typeParameters!!
    }

    private val invokeSymbol = irClass.declarations
        .single { it.nameForIrSerialization.asString() == "invoke" }
        .cast<IrSimpleFunction>()
        .getLastOverridden().symbol

    fun getArity(): Int? {
        return irClass.name.asString().removePrefix("Function").removePrefix("KFunction").toIntOrNull()
    }

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        return if (invokeSymbol == expression.symbol) irFunction else null
    }

    private fun isLambda(): Boolean = irFunction.name.let { it == Name.special("<anonymous>") || it == Name.special("<no name provided>") }

    override fun toString(): String {
        return if (isLambda()) {
            val receiver = (irFunction.dispatchReceiverParameter?.type ?: irFunction.extensionReceiverParameter?.type)?.renderType()
            val arguments = irFunction.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.renderType() }
            val returnType = irFunction.returnType.renderType()
            ("$arguments -> $returnType").let { if (receiver != null) "$receiver.$it" else it }
        } else {
            val dispatchReceiver = irFunction.parentAsClass.defaultType // = instanceReceiverParameter
            val extensionReceiver = irFunction.extensionReceiverParameter?.type
            val receivers = if (irFunction is IrConstructor) "" else renderReceivers(dispatchReceiver, extensionReceiver)
            val arguments = irFunction.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.renderType() }
            val returnType = irFunction.returnType.renderType()
            "fun $receivers${irFunction.name}$arguments: $returnType"
        }
    }
}

internal class KTypeState(val irType: IrType) : ReflectionState(irType.classifierOrFail) {
    private var _arguments: List<KTypeProjection>? = null

    fun getArguments(interpreter: IrInterpreter): List<KTypeProjection> {
        if (_arguments != null) return _arguments!!
        _arguments = (irType as IrSimpleType).arguments
            .map {
                when (it.getVariance()) {
                    Variance.INVARIANT -> KTypeProjection.invariant(KTypeProxy(KTypeState(it.typeOrNull!!), interpreter))
                    Variance.IN_VARIANCE -> KTypeProjection.contravariant(KTypeProxy(KTypeState(it.typeOrNull!!), interpreter))
                    Variance.OUT_VARIANCE -> KTypeProjection.covariant(KTypeProxy(KTypeState(it.typeOrNull!!), interpreter))
                    null -> KTypeProjection.STAR
                }
            }
        return _arguments!!
    }

    private fun IrTypeArgument.getVariance(): Variance? {
        return when (this) {
            is IrSimpleType -> Variance.INVARIANT
            is IrTypeProjection -> this.variance
            is IrStarProjection -> null
            else -> TODO()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KTypeState

        if (irType != other.irType) return false

        return true
    }

    override fun hashCode(): Int {
        return irType.hashCode()
    }

    override fun toString(): String {
        return irType.renderType()
    }
}

internal class KTypeParameterState(val irTypeParameter: IrTypeParameter) : ReflectionState(irTypeParameter) {
    private var _upperBounds: List<KType>? = null

    fun getUpperBounds(interpreter: IrInterpreter): List<KType> {
        if (_upperBounds != null) return _upperBounds!!
        _upperBounds = irTypeParameter.superTypes.map { KTypeProxy(KTypeState(it), interpreter) }
        return _upperBounds!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KTypeParameterState

        if (irTypeParameter != other.irTypeParameter) return false

        return true
    }

    override fun hashCode(): Int {
        return irTypeParameter.hashCode()
    }

    override fun toString(): String {
        return irTypeParameter.name.asString()
    }
}

internal class KParameterState(val irType: IrType) : ReflectionState(irType.classifierOrFail) {

}