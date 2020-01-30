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
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.name.FqNameUnsafe
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

interface State {
    val fields: MutableList<Variable>
    val irClass: IrClass

    fun getState(descriptor: DeclarationDescriptor): State? {
        return fields.firstOrNull { it.descriptor.equalTo(descriptor) }?.state
    }

    fun setState(newVar: Variable)

    /**
     * This method is used for passing a copy of a state.
     * It is necessary then copy change its state's value, but the original one must remain the same.
     *
     * @see copyReceivedValue.kt
     * @see tryFinally.kt
     */
    fun copy(): State

    fun getIrFunction(descriptor: FunctionDescriptor): IrFunction?
}

class Primitive<T>(var value: T, val type: IrType) : State {
    override val fields: MutableList<Variable> = mutableListOf()
    override val irClass: IrClass = type.classOrNull!!.owner

    init {
        val properties = irClass.declarations.filterIsInstance<IrProperty>()
        for (property in properties) {
            val propertySignature = CompileTimeFunction(property.name.asString(), listOf(irClass.descriptor.defaultType.toString()))
            val propertyValue = unaryFunctions[propertySignature]?.invoke(value)
                ?: throw NoSuchMethodException("For given property $propertySignature there is no entry in unary map")
            fields.add(Variable(property.descriptor, Primitive(propertyValue, property.backingField!!.type)))
        }
    }

    override fun setState(newVar: Variable) {
        newVar.state as? Primitive<T> ?: throw IllegalArgumentException("Cannot set $newVar in current $this")
        value = newVar.state.value
    }

    override fun copy(): State {
        return Primitive(value, type)
    }

    override fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        // must add property's getter to declaration's list because they are not present in ir class for primitives
        val declarations = irClass.declarations.map { if (it is IrProperty) it.getter else it }
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
        return "Primitive(value=$value, type=${irClass.descriptor.defaultType})"
    }
}

abstract class Complex(override var irClass: IrClass, override val fields: MutableList<Variable>) : State {
    var superType: Complex? = null
    var instance: Complex? = null

    fun setInstanceRecursive(instance: Complex) {
        this.instance = instance
        superType?.setInstanceRecursive(instance)
    }

    fun getReceiver(): DeclarationDescriptor {
        return irClass.thisReceiver!!.descriptor
    }

    override fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        return irClass.declarations.filterIsInstance<IrFunction>()
            .filter { it.descriptor.name == descriptor.name }
            .firstOrNull { it.descriptor.valueParameters.map { it.type } == descriptor.valueParameters.map { it.type } }
    }
}

class Common(override var irClass: IrClass, override val fields: MutableList<Variable>) : Complex(irClass, fields) {
    override fun setState(newVar: Variable) {
        when (val oldState = fields.firstOrNull { it.descriptor == newVar.descriptor }) {
            null -> fields.add(newVar)                          // newVar isn't present in value list
            else -> fields[fields.indexOf(oldState)] = newVar   // newVar already present
        }
    }

    fun getToStringFunction(): IrFunctionImpl {
        return irClass.declarations.filterIsInstance<IrFunction>()
            .filter { it.descriptor.name.asString() == "toString" }
            .first { it.descriptor.valueParameters.isEmpty() } as IrFunctionImpl
    }

    override fun copy(): State {
        return Common(irClass, fields).apply {
            this@apply.superType = this@Common.superType
            this@apply.instance = this@Common.instance
        }
    }

    override fun toString(): String {
        return "Common(obj='${irClass.fqNameForIrSerialization}', super=$superType, values=$fields)"
    }
}

class Wrapper(val value: Any, override var irClass: IrClass) : Complex(irClass, mutableListOf()) {
    private val typeFqName = irClass.fqNameForIrSerialization.toUnsafe()
    private val receiverClass = irClass.defaultType.getClass(true)

    fun getMethod(irFunction: IrFunction): MethodHandle {
        // if function is actually a getter, then use property name as method name
        val property = (irFunction as? IrFunctionImpl)?.correspondingPropertySymbol?.owner

        // intrinsicName is used to get correct java method
        // for example: - method 'get' in kotlin StringBuilder is actually 'charAt' in java StringBuilder
        //              - use getter for private fields such as detailMessage in java.lang.Throwable
        val intrinsicName = property?.getEvaluateIntrinsicValue() ?: irFunction.getEvaluateIntrinsicValue()
        val methodName = intrinsicName ?: (property ?: irFunction).name.toString()

        val methodType = irFunction.getMethodType()
        return MethodHandles.lookup().findVirtual(receiverClass, methodName, methodType)
    }

    override fun setState(newVar: Variable) {
        throw UnsupportedOperationException("Method setState is not supported in Wrapper class")
    }

    companion object {
        fun getConstructorMethod(irConstructor: IrFunction): MethodHandle {
            val methodType = irConstructor.getMethodType()

            return MethodHandles.lookup().findConstructor(irConstructor.returnType.getClass(true), methodType)
        }

        fun getStaticMethod(irFunction: IrFunction): MethodHandle {
            val jvmClassName = Class.forName(irFunction.getEvaluateIntrinsicValue()!!)

            val methodType = irFunction.getMethodType()
            return MethodHandles.lookup().findStatic(jvmClassName, irFunction.name.asString(), methodType)
        }

        private fun IrFunction.getMethodType(): MethodType {
            val argsClasses = this.valueParameters.map { it.type.getClass(this.isValueParameterPrimitiveAsObject(it.index)) }
            return if (this is IrFunctionImpl) {
                // for regular methods and functions
                val returnClass = this.returnType.getClass(this.isReturnTypePrimitiveAsObject())
                val extensionClass = this.extensionReceiverParameter?.type?.getClass(this.isExtensionReceiverPrimitive())

                MethodType.methodType(returnClass, listOfNotNull(extensionClass) + argsClasses)
            } else {
                // for constructors
                MethodType.methodType(Void::class.javaPrimitiveType, argsClasses)
            }
        }

        private fun IrType.getClass(asObject: Boolean): Class<out Any> {
            val fqName = this.getFqName()
            val owner = this.classOrNull?.owner
            return when {
                owner.hasAnnotation(evaluateIntrinsicAnnotation) -> Class.forName(owner!!.getEvaluateIntrinsicValue())
                this.isPrimitiveType() -> getPrimitiveClass(fqName!!, asObject)
                this.isArray() -> if (asObject) Array<Any?>::class.javaObjectType else Array<Any?>::class.java
                //TODO primitive array
                this.isTypeParameter() -> Any::class.java
                else -> JavaToKotlinClassMap.mapKotlinToJava(FqNameUnsafe(fqName!!))?.let { Class.forName(it.asSingleFqName().toString()) }
            } ?: Class.forName(fqName)
        }

        private fun IrFunction.getOriginalOverriddenSymbols(): MutableList<IrFunctionSymbol> {
            val overriddenSymbols = mutableListOf<IrFunctionSymbol>()
            if (this is IrFunctionImpl) {
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
            }

            if (overriddenSymbols.isEmpty()) overriddenSymbols.add(this.symbol)
            return overriddenSymbols
        }

        private fun IrFunction.isExtensionReceiverPrimitive(): Boolean {
            return this.extensionReceiverParameter?.type?.isPrimitiveType() == false
        }

        private fun IrFunction.isReturnTypePrimitiveAsObject(): Boolean {
            for (symbol in getOriginalOverriddenSymbols()) {
                if (!symbol.owner.returnType.isTypeParameter() && !symbol.owner.returnType.isNullable()) {
                    return false
                }
            }
            return true
        }

        private fun IrFunction.isValueParameterPrimitiveAsObject(index: Int): Boolean {
            for (symbol in getOriginalOverriddenSymbols()) {
                if (!symbol.owner.valueParameters[index].type.isTypeParameter() && !symbol.owner.valueParameters[index].type.isNullable()) {
                    return false
                }
            }
            return true
        }
    }

    override fun copy(): State {
        return Wrapper(value, irClass).apply { this@apply.instance = this@Wrapper.instance }
    }

    override fun toString(): String {
        return "Wrapper(obj='$typeFqName', value=$value)"
    }
}

class Lambda(val irFunction: IrFunction, override var irClass: IrClass) : Complex(irClass, mutableListOf()) {
    // irFunction is anonymous declaration, but irCall will contain descriptor of invoke method from Function interface
    private val invokeDescriptor = irClass.declarations.first().descriptor

    override fun setState(newVar: Variable) {
        throw UnsupportedOperationException("Method setState is not supported in Lambda class")
    }

    override fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        return if (invokeDescriptor.equalTo(descriptor)) irFunction else null
    }

    override fun copy(): State {
        return Lambda(irFunction, irClass).apply { this@apply.instance = this@Lambda.instance }
    }

    override fun toString(): String {
        return "Lambda(${irClass.fqNameForIrSerialization})"
    }
}