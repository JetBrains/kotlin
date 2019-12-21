/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.backend.common.interpreter.*
import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
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
        val methodName = irFunction.name.toString()

        val returnClass = irFunction.returnType.getCorrespondingClass(irFunction.isReturnTypePrimitiveAsObject())
        val argsClasses = irFunction.valueParameters.map { it.type.getCorrespondingClass(irFunction.isValueParameterPrimitiveAsObject(it.index)) }
        val argsValues = data.getAll().map { (it.state as? Wrapper)?.wrapperValue ?: (it.state as? Primitive<*>)?.value }

        val methodType = MethodType.methodType(returnClass, argsClasses)
        val method = MethodHandles.lookup().findVirtual(receiverClass, methodName, methodType)

        val result = method.invokeWithArguments(argsValues)
        data.pushReturnValue(result.toState(irFunction.returnType))

        return Code.NEXT
    }

    private fun IrType.getCorrespondingClass(asObject: Boolean): Class<out Any> {
        val fqName = this.getFqName()
        return when {
            this.isPrimitiveType() -> getPrimitiveClass(fqName!!, asObject)
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

    override fun copy(): State {
        return Wrapper(type, wrapperValue)
    }

    override fun toString(): String {
        return "Wrapper(obj='$typeFqName', value=$wrapperValue)"
    }
}
