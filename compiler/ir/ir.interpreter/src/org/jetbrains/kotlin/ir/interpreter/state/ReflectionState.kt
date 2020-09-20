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
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KFunctionProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KProperty1Proxy
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction

internal abstract class ReflectionState(override val irClass: IrClass) : State {
    override val fields: MutableList<Variable> = mutableListOf()
    override val typeArguments: MutableList<Variable> = mutableListOf()

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? = null

    protected fun renderReceivers(dispatchReceiver: IrType?, extensionReceiver: IrType?): String {
        return buildString {
            if (dispatchReceiver != null) {
                append(dispatchReceiver.render())
                append(".")
            }
            val addParentheses = dispatchReceiver != null && extensionReceiver != null
            if (addParentheses) append("(")
            if (extensionReceiver != null) {
                append(extensionReceiver.render())
                append(".")
            }
            if (addParentheses) append(")")
        }
    }
}

internal class KClassState(override val irClass: IrClass) : ReflectionState(irClass) {
    private var _members: Collection<KCallable<*>>? = null
    private var _constructors: Collection<KFunction<Proxy>>? = null

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
}

internal class KPropertyState(
    val property: IrProperty, val dispatchReceiver: State?, val extensionReceiver: State?
) : ReflectionState(property.parentAsClass) {
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
        val returnType = property.getter!!.returnType.render()
        return "$prefix $receivers${property.name}: $returnType"
    }
}

internal class KFunctionState(val irFunction: IrFunction, override val irClass: IrClass) : ReflectionState(irClass) {
    override val fields: MutableList<Variable> = mutableListOf()
    override val typeArguments: MutableList<Variable> = mutableListOf()

    constructor(functionReference: IrFunctionReference) : this(functionReference.symbol.owner, functionReference.type.classOrNull!!.owner)
    constructor(irFunction: IrFunction, functionFactory: IrAbstractFunctionFactory) :
            this(irFunction, functionFactory.functionN(irFunction.valueParameters.size))

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
            val receiver = (irFunction.dispatchReceiverParameter?.type ?: irFunction.extensionReceiverParameter?.type)?.render()
            val arguments = irFunction.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.render() }
            val returnType = irFunction.returnType.render()
            ("$arguments -> $returnType").let { if (receiver != null) "$receiver.$it" else it }
        } else {
            val dispatchReceiver = irFunction.parentAsClass.defaultType // = instanceReceiverParameter
            val extensionReceiver = irFunction.extensionReceiverParameter?.type
            val receivers = if (irFunction is IrConstructor) "" else renderReceivers(dispatchReceiver, extensionReceiver)
            val arguments = irFunction.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.render() }
            val returnType = irFunction.returnType.render()
            "fun $receivers${irFunction.name}$arguments: $returnType"
        }
    }
}