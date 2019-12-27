/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.backend.common.interpreter.*
import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.evaluateIntrinsicAnnotation
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.name.FqNameUnsafe
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

interface State {
    val fields: MutableList<Variable>

    fun getState(descriptor: DeclarationDescriptor): State? {
        return fields.firstOrNull { it.descriptor.equalTo(descriptor) }?.state
    }

    fun setState(newVar: Variable)
    fun copy(): State
    fun getIrFunction(descriptor: FunctionDescriptor): IrFunction?
}

open class Primitive<T>(var value: T, val type: IrType) : State {
    final override val fields: MutableList<Variable> = mutableListOf()

    init {
        val properties = type.classOrNull!!.owner.declarations.filterIsInstance<IrProperty>()
        for (property in properties) {
            val propertySignature = CompileTimeFunction(property.name.asString(), listOf(type.getName()))
            val propertyValue = unaryFunctions[propertySignature]?.invoke(value)
                ?: throw NoSuchMethodException("For given property $propertySignature there is no entry in unary map")
            fields.add(Variable(property.descriptor, Primitive(propertyValue, property.backingField!!.type)))
        }
    }

    private fun IrType.getName() = this.classOrNull!!.descriptor.defaultType.toString()

    override fun setState(newVar: Variable) {
        newVar.state as? Primitive<T> ?: throw IllegalArgumentException("Cannot set $newVar in current $this")
        value = newVar.state.value
    }

    override fun copy(): State {
        return Primitive(value, type)
    }

    override fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        // must add property's getter to declaration's list because they are not present in ir class for primitives
        val declarations = type.classOrNull!!.owner.declarations.map { if (it is IrProperty) it.getter else it }
        return declarations.filterIsInstance<IrFunction>()
            .filter { it.descriptor.name == descriptor.name }
            .firstOrNull { it.descriptor.valueParameters.map { it.type } == descriptor.valueParameters.map { it.type } }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Primitive<*>

        if (value != other.value) return false
        if (type != other.type) return false
        if (fields != other.fields) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value?.hashCode() ?: 0
        result = 31 * result + type.hashCode()
        result = 31 * result + fields.hashCode()
        return result
    }

    override fun toString(): String {
        return "Primitive(value=$value, type=${type.getName()})"
    }
}

open class Complex(private var type: IrClass, override val fields: MutableList<Variable>) : State {
    var superType: Complex? = null
    var instance: Complex? = null

    fun setInstanceRecursive(instance: Complex) {
        this.instance = instance
        superType?.setInstanceRecursive(instance)
    }

    fun getReceiver(): DeclarationDescriptor {
        return type.thisReceiver!!.descriptor
    }

    override fun setState(newVar: Variable) {
        when (val oldState = fields.firstOrNull { it.descriptor == newVar.descriptor }) {
            null -> fields.add(newVar)                          // newVar isn't present in value list
            else -> fields[fields.indexOf(oldState)] = newVar   // newVar already present
        }
    }

    override fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        return type.declarations.filterIsInstance<IrFunction>()
            .filter { it.descriptor.name == descriptor.name }
            .firstOrNull { it.descriptor.valueParameters.map { it.type } == descriptor.valueParameters.map { it.type } }
    }

    override fun copy(): State {
        return Complex(type, fields).apply {
            this@apply.superType = this@Complex.superType
            this@apply.instance = this@Complex.instance
        }
    }

    override fun toString(): String {
        return "Complex(obj='${type.fqNameForIrSerialization}', super=$superType, values=$fields)"
    }
}

class Wrapper(private val type: IrClass, private val wrapperValue: Any) : Complex(type, mutableListOf()) {
    private val typeFqName = type.fqNameForIrSerialization.toUnsafe()
    private val receiverClass = getPrimitiveClass(typeFqName.toString(), asObject = true)
        ?: JavaToKotlinClassMap.mapKotlinToJava(typeFqName)?.let { Class.forName(it.asSingleFqName().toString()) }
        ?: Class.forName(typeFqName.toString())

    fun invoke(irFunction: IrFunctionImpl, data: Frame): Code {
        val methodName = (irFunction.correspondingPropertySymbol?.owner?.name ?: irFunction.name).toString()

        val methodType = irFunction.getMethodType()
        val method = MethodHandles.lookup().findVirtual(receiverClass, methodName, methodType)

        val result = method.invokeWithArguments(irFunction.getArgs(data))
        data.pushReturnValue(result.toState(irFunction.returnType))

        return Code.NEXT
    }

    companion object {
        fun invokeStatic(irFunction: IrFunctionImpl, data: Frame): Code {
            val annotation = irFunction.getAnnotation(evaluateIntrinsicAnnotation)
            val jvmFileName = Class.forName((annotation.getValueArgument(0) as IrConst<*>).value.toString())

            val methodType = irFunction.getMethodType(withExtensionReceiver = true)
            val method = MethodHandles.lookup().findStatic(jvmFileName, irFunction.name.asString(), methodType)

            val result = method.invokeWithArguments(irFunction.getArgs(data))
            data.pushReturnValue(result.toState(irFunction.returnType))

            return Code.NEXT
        }

        private fun IrFunctionImpl.getMethodType(withExtensionReceiver: Boolean = false): MethodType {
            val returnClass = this.returnType.getClass(this.isReturnTypePrimitiveAsObject())
            val argsClasses = this.valueParameters.map { it.type.getClass(this.isValueParameterPrimitiveAsObject(it.index)) }

            val extensionClass = when {
                withExtensionReceiver -> this.extensionReceiverParameter?.type?.getClass(this.isExtensionReceiverPrimitiveAsObject())
                else -> null
            }

            return MethodType.methodType(returnClass, listOfNotNull(extensionClass) + argsClasses)
        }

        private fun IrFunctionImpl.getArgs(data: Frame): List<Any?> {
            val argsValues = data.getAll().map { (it.state as? Wrapper)?.wrapperValue ?: (it.state as? Any) }.toMutableList()

            if (this.extensionReceiverParameter?.type.isPrimitiveState()) {
                val varargState = data.getVariableState(this.symbol.getExtensionReceiver()!!)
                argsValues[0] = (varargState as Primitive<*>).value
            }

            for ((index, valueParameter) in this.valueParameters.withIndex().reversed()) {
                if (valueParameter.type.isPrimitiveState()) {
                    argsValues[argsValues.size - 1 - index] = (argsValues[argsValues.size - 1 - index] as Primitive<*>).value
                }
            }

            // TODO if vararg isn't last parameter
            // must convert vararg array into separated elements for correct invoke
            if (this.valueParameters.lastOrNull()?.varargElementType != null) {
                argsValues.removeAt(argsValues.size - 1)
                val varargState = data.getVariableState(this.valueParameters.last().descriptor)
                val varargValue = (varargState as? Wrapper)?.wrapperValue ?: (varargState as Primitive<*>).value
                argsValues.addAll(varargValue as Array<out Any?>)
            }

            return argsValues
        }

        private fun IrType.getClass(asObject: Boolean): Class<out Any> {
            val fqName = this.getFqName()
            return when {
                this.isPrimitiveType() -> getPrimitiveClass(fqName!!, asObject)
                this.isArray() -> if (asObject) Array<Any?>::class.javaObjectType else Array<Any?>::class.java
                //TODO primitive array
                this.isTypeParameter() -> Any::class.java
                else -> JavaToKotlinClassMap.mapKotlinToJava(FqNameUnsafe(fqName!!))?.let { Class.forName(it.asSingleFqName().toString()) }
            } ?: Class.forName(fqName)
        }

        private fun IrFunctionImpl.getOriginalOverriddenSymbols(): MutableList<IrSimpleFunctionSymbol> {
            val overriddenSymbols = mutableListOf<IrSimpleFunctionSymbol>()
            val pool = this.overriddenSymbols.toMutableList()
            val iterator = pool.listIterator()
            for (symbol in iterator) {
                if (symbol.owner.overriddenSymbols.isEmpty()) {
                    overriddenSymbols += symbol
                    iterator.remove()
                } else {
                    symbol.owner.overriddenSymbols.forEach { iterator.add(it) }
                }
            }

            if (overriddenSymbols.isEmpty()) overriddenSymbols.add(this.symbol)
            return overriddenSymbols
        }

        private fun IrFunctionImpl.isExtensionReceiverPrimitiveAsObject(): Boolean {
            return this.extensionReceiverParameter?.type?.isPrimitiveType() == false
        }

        private fun IrFunctionImpl.isReturnTypePrimitiveAsObject(): Boolean {
            for (symbol in getOriginalOverriddenSymbols()) {
                if (!symbol.owner.returnType.isTypeParameter() && !symbol.owner.returnType.isNullable()) {
                    return false
                }
            }
            return true
        }

        private fun IrFunctionImpl.isValueParameterPrimitiveAsObject(index: Int): Boolean {
            for (symbol in getOriginalOverriddenSymbols()) {
                if (!symbol.owner.valueParameters[index].type.isTypeParameter() && !symbol.owner.valueParameters[index].type.isNullable()) {
                    return false
                }
            }
            return true
        }
    }

    override fun copy(): State {
        return Wrapper(type, wrapperValue)
    }

    override fun toString(): String {
        return "Wrapper(obj='$typeFqName', value=$wrapperValue)"
    }
}
