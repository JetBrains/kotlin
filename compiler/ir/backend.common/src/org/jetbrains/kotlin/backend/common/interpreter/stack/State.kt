/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.backend.common.interpreter.*
import org.jetbrains.kotlin.backend.common.interpreter.builtins.evaluateIntrinsicAnnotation
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqNameUnsafe
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.math.min

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

    override fun getState(descriptor: DeclarationDescriptor): State {
        return super.getState(descriptor) ?: this
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

abstract class Complex(override val irClass: IrClass, override val fields: MutableList<Variable>) : State {
    var superType: Complex? = null
    var instance: Complex? = null

    fun setInstanceRecursive(instance: Complex) {
        this.instance = instance
        superType?.setInstanceRecursive(instance)
    }

    fun getReceiver(): DeclarationDescriptor {
        return irClass.thisReceiver!!.descriptor
    }

    fun irClassFqName(): String {
        return irClass.fqNameForIrSerialization.toString()
    }

    override fun setState(newVar: Variable) {
        when (val oldState = fields.firstOrNull { it.descriptor == newVar.descriptor }) {
            null -> fields.add(newVar)                          // newVar isn't present in value list
            else -> fields[fields.indexOf(oldState)] = newVar   // newVar already present
        }
    }

    fun setStatesFrom(state: Complex) = state.fields.forEach { setState(it) }

    override fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        if (descriptor is PropertyGetterDescriptor)
            return irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.getter }.single { it.descriptor.equalTo(descriptor) }

        return irClass.declarations.filterIsInstance<IrFunction>().singleOrNull { it.descriptor.equalTo(descriptor) }
    }
}

class Common(override val irClass: IrClass, override val fields: MutableList<Variable>) : Complex(irClass, fields) {
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

class Wrapper(val value: Any, override val irClass: IrClass) : Complex(irClass, mutableListOf()) {
    private val typeFqName = irClass.fqNameForIrSerialization.toUnsafe()
    private val receiverClass = irClass.defaultType.getClass(true)

    init {
        instance = this
    }

    fun getMethod(irFunction: IrFunction): MethodHandle? {
        // if function is actually a getter, then use "get${property.name.capitalize()}" as method name
        val propertyName = (irFunction as? IrFunctionImpl)?.correspondingPropertySymbol?.owner?.name?.asString()
        val propertyExplicitCall = propertyName?.takeIf { receiverClass.methods.map { it.name }.contains(it) }
        val propertyGetCall = "get${propertyName?.capitalize()}".takeIf { receiverClass.methods.map { it.name }.contains(it) }

        // intrinsicName is used to get correct java method
        // for example: - method 'get' in kotlin StringBuilder is actually 'charAt' in java StringBuilder
        val intrinsicName = irFunction.getEvaluateIntrinsicValue()
        if (intrinsicName?.isEmpty() == true) return null
        val methodName = intrinsicName ?: propertyExplicitCall ?: propertyGetCall ?: irFunction.name.toString()

        val methodType = irFunction.getMethodType()
        return MethodHandles.lookup().findVirtual(receiverClass, methodName, methodType)
    }

    override fun setState(newVar: Variable) {
        throw UnsupportedOperationException("Method setState is not supported in Wrapper class")
    }

    companion object {
        private val companionObjectValue = mapOf<String, Any>("kotlin.text.Regex\$Companion" to Regex.Companion)

        fun getCompanionObject(irClass: IrClass): Wrapper {
            val objectName = irClass.getEvaluateIntrinsicValue()!!
            val objectValue = companionObjectValue[objectName] ?: throw AssertionError("Companion object $objectName cannot be interpreted")
            return Wrapper(objectValue, irClass)
        }

        fun getConstructorMethod(irConstructor: IrFunction): MethodHandle {
            val methodType = irConstructor.getMethodType()

            return MethodHandles.lookup().findConstructor(irConstructor.returnType.getClass(true), methodType)
        }

        fun getStaticMethod(irFunction: IrFunction): MethodHandle? {
            val intrinsicName = irFunction.getEvaluateIntrinsicValue()
            if (intrinsicName?.isEmpty() == true) return null
            val jvmClassName = Class.forName(intrinsicName!!)

            val methodType = irFunction.getMethodType()
            return MethodHandles.lookup().findStatic(jvmClassName, irFunction.name.asString(), methodType)
        }

        fun getEnumEntry(enumClass: IrClass): MethodHandle? {
            val intrinsicName = enumClass.getEvaluateIntrinsicValue()
            if (intrinsicName?.isEmpty() == true) return null
            val enumClassName = Class.forName(intrinsicName!!)

            val methodType = MethodType.methodType(enumClassName, String::class.java)
            return MethodHandles.lookup().findStatic(enumClassName, "valueOf", methodType)
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
                this.isPrimitiveType() -> getPrimitiveClass(fqName!!, asObject)
                this.isArray() -> if (asObject) Array<Any?>::class.javaObjectType else Array<Any?>::class.java
                owner.hasAnnotation(evaluateIntrinsicAnnotation) -> Class.forName(owner!!.getEvaluateIntrinsicValue())
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

class Lambda(val irFunction: IrFunction, override val irClass: IrClass) : Complex(irClass, mutableListOf()) {
    // irFunction is anonymous declaration, but irCall will contain descriptor of invoke method from Function interface
    private val invokeDescriptor = irClass.declarations.single { it.nameForIrSerialization.asString() == "invoke" }.descriptor

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

class ExceptionState private constructor(
    override val irClass: IrClass, override val fields: MutableList<Variable>, stackTrace: List<String>
) : Complex(irClass, fields) {
    private lateinit var exceptionFqName: String
    private val exceptionHierarchy = mutableListOf<String>()
    private val messageProperty = irClass.getPropertyByName("message")
    private val causeProperty = irClass.getPropertyByName("cause")

    private val stackTrace: List<String>

    init {
        instance = this
        this.stackTrace = stackTrace.reversed()
        if (!this::exceptionFqName.isInitialized) this.exceptionFqName = irClassFqName()

        if (fields.none { it.descriptor.equalTo(messageProperty.descriptor) }) {
            setMessage()
        }
    }

    constructor(common: Common, stackTrace: List<String>) : this(common.irClass, common.fields, stackTrace) {
        var wrapperSuperType: Complex? = common
        while (wrapperSuperType != null && wrapperSuperType !is Wrapper) wrapperSuperType = (wrapperSuperType as Common).superType
        setUpCauseIfNeeded(wrapperSuperType as? Wrapper)
    }

    constructor(wrapper: Wrapper, stackTrace: List<String>) : this(wrapper.value as Throwable, wrapper.irClass, stackTrace) {
        setUpCauseIfNeeded(wrapper)
    }

    constructor(
        exception: Throwable, irClass: IrClass, stackTrace: List<String>
    ) : this(irClass, evaluateFields(exception, irClass, stackTrace), stackTrace + evaluateAdditionalStackTrace(exception)) {
        if (irClass.name.asString() != exception::class.java.simpleName) {
            // ir class wasn't found in classpath, a stub was passed => need to save java class hierarchy
            this.exceptionFqName = exception::class.java.name
            exceptionHierarchy += this.exceptionFqName
            generateSequence(exception::class.java.superclass) { it.superclass }.forEach { exceptionHierarchy += it.name }
            exceptionHierarchy.removeAt(exceptionHierarchy.lastIndex) // remove unnecessary java.lang.Object
        }
    }

    private fun setUpCauseIfNeeded(wrapper: Wrapper?) {
        val cause = (wrapper?.value as? Throwable)?.cause as? ExceptionData
        setCause(cause?.state)
        if (getMessage().value == null && cause != null) {
            val causeMessage = cause.state.exceptionFqName + (cause.state.getMessage().value?.let { ": $it" } ?: "")
            setMessage(causeMessage)
        }
    }

    fun isSubtypeOf(ancestor: IrClass): Boolean {
        if (exceptionHierarchy.isNotEmpty()) {
            return exceptionHierarchy.any { it.contains(ancestor.name.asString()) }
        }
        return irClass.isSubclassOf(ancestor)
    }

    private fun setMessage(messageValue: String? = null) {
        setState(Variable(messageProperty.descriptor, Primitive(messageValue, messageProperty.getter!!.returnType)))
    }

    private fun setCause(causeValue: State?) {
        setState(Variable(causeProperty.descriptor, causeValue ?: Primitive<Throwable?>(null, causeProperty.getter!!.returnType)))
    }

    fun getMessage(): Primitive<String?> = getState(messageProperty.descriptor) as Primitive<String?>
    fun getMessageWithName(): String = getMessage().value?.let { "$exceptionFqName: $it" } ?: exceptionFqName

    fun getCause(): ExceptionState? = getState(causeProperty.descriptor)?.let { if (it is ExceptionState) it else null }

    fun getFullDescription(): String {
        // TODO remainder of the stack trace with "..."
        val message = getMessage().value.let { if (it?.isNotEmpty() == true) ": $it" else "" }
        val prefix = if (stackTrace.isNotEmpty()) "\n\t" else ""
        val postfix = if (stackTrace.size > 10) "\n\t..." else ""
        val causeMessage = getCause()?.getFullDescription()?.replaceFirst("Exception ", "\nCaused by: ") ?: ""
        return "Exception $exceptionFqName$message" +
                stackTrace.subList(0, min(stackTrace.size, 10)).joinToString(separator = "\n\t", prefix = prefix, postfix = postfix) +
                causeMessage
    }

    fun getThisAsCauseForException() = ExceptionData(this)

    override fun copy(): State {
        return ExceptionState(irClass, fields, stackTrace).apply { this@apply.instance = this@ExceptionState.instance }
    }

    companion object {
        private fun IrClass.getPropertyByName(name: String): IrProperty {
            val getPropertyFun = this.declarations.firstOrNull { it.nameForIrSerialization.asString().contains("get-$name") }
            return (getPropertyFun as? IrFunctionImpl)?.correspondingPropertySymbol?.owner
                ?: this.declarations.single { it.nameForIrSerialization.asString() == name } as IrProperty
        }

        private fun evaluateFields(exception: Throwable, irClass: IrClass, stackTrace: List<String>): MutableList<Variable> {
            val messageProperty = irClass.getPropertyByName("message")
            val causeProperty = irClass.getPropertyByName("cause")

            val messageVar = Variable(messageProperty.descriptor, exception.message.toState(messageProperty.getter!!.returnType))
            val causeVar = exception.cause?.let {
                Variable(causeProperty.descriptor, ExceptionState(it, irClass, stackTrace + it.stackTrace.reversed().map { "at $it" }))
            }
            return listOfNotNull(messageVar, causeVar).toMutableList()
        }

        private fun evaluateAdditionalStackTrace(e: Throwable): List<String> {
            // TODO do we really need this?... It will point to JVM stdlib
            val additionalStack = mutableListOf<String>()
            if (e.stackTrace.any { it.className == "java.lang.invoke.MethodHandle" }) {
                for ((index, stackTraceElement) in e.stackTrace.withIndex()) {
                    if (stackTraceElement.methodName == "invokeWithArguments") {
                        additionalStack.addAll(e.stackTrace.slice(0 until index).reversed().map { "at $it" })
                        break
                    }
                }

                var cause = e.cause
                val lastNeededValue = e.stackTrace.first().let { it.className + "." + it.methodName }
                while (cause != null) {
                    for ((causeStackIndex, causeStackTraceElement) in cause.stackTrace.withIndex()) {
                        val currentStackTraceValue = causeStackTraceElement.let { it.className + "." + it.methodName }
                        if (currentStackTraceValue == lastNeededValue) {
                            cause.stackTrace = cause.stackTrace.sliceArray(0 until causeStackIndex).reversedArray()
                            break
                        }
                    }
                    cause = cause.cause
                }
            }
            return additionalStack
        }
    }
}

// TODO remove this data class and make ExceptionState a child of Throwable
// this is possible by converting Complex to an interface
data class ExceptionData(val state: ExceptionState) : Throwable() {
    override val message: String? = state.getMessage().value
    override fun fillInStackTrace() = this

    override fun toString(): String = state.getMessageWithName()
}