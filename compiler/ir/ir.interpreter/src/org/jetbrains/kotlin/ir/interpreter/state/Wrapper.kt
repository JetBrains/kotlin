/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.interpreter.builtins.evaluateIntrinsicAnnotation
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

internal class Wrapper(val value: Any, override val irClass: IrClass) : Complex {
    override val fields: MutableList<Variable> = mutableListOf()

    override var superWrapperClass: Wrapper? = null
    override val typeArguments: MutableList<Variable> = mutableListOf()
    override var outerClass: Variable? = null

    private val receiverClass = irClass.defaultType.getClass(true)

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? = null

    fun getMethod(irFunction: IrFunction): MethodHandle? {
        if (irFunction.getEvaluateIntrinsicValue()?.isEmpty() == true) return null // this method will handle IntrinsicEvaluator
        // if function is actually a getter, then use "get${property.name.capitalize()}" as method name
        val propertyName = (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.name?.asString()
        val propertyCall = listOfNotNull(propertyName, "get${propertyName?.capitalizeAsciiOnly()}")
            .firstOrNull { receiverClass.methods.any { method -> method.name == it } }

        val intrinsicName = getJavaOriginalName(irFunction)
        val methodName = intrinsicName ?: propertyCall ?: irFunction.name.toString()
        val methodType = irFunction.getMethodType()
        return MethodHandles.lookup().findVirtual(receiverClass, methodName, methodType)
    }

    // This method is used to get correct java method name
    private fun getJavaOriginalName(irFunction: IrFunction): String? {
        return when (irFunction.getLastOverridden().fqNameWhenAvailable?.asString()) {
            "kotlin.collections.Map.<get-entries>" -> "entrySet"
            "kotlin.collections.Map.<get-keys>" -> "keySet"
            "kotlin.CharSequence.get" -> "charAt"
            "kotlin.collections.MutableList.removeAt" -> "remove"
            else -> null
        }
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        private val companionObjectValue = mapOf<String, Any>("kotlin.text.Regex\$Companion" to Regex.Companion)

        fun getReflectionMethod(irFunction: IrFunction): MethodHandle {
            val receiverClass = irFunction.dispatchReceiverParameter!!.type.getClass(asObject = true)
            val methodType = irFunction.getMethodType()
            val methodName = when (irFunction) {
                is IrSimpleFunction -> {
                    val property = irFunction.correspondingPropertySymbol?.owner
                    when {
                        property?.getter == irFunction -> "get${property.name.asString().capitalizeAsciiOnly()}"
                        property?.setter == irFunction -> "set${property.name.asString().capitalizeAsciiOnly()}"
                        else -> irFunction.name.asString()
                    }
                }
                else -> irFunction.name.asString()
            }
            return MethodHandles.lookup().findVirtual(receiverClass, methodName, methodType)
        }

        fun getCompanionObject(irClass: IrClass): Wrapper {
            val objectName = irClass.getEvaluateIntrinsicValue()!!
            val objectValue = companionObjectValue[objectName] ?: throw InternalError("Companion object $objectName cannot be interpreted")
            return Wrapper(objectValue, irClass)
        }

        fun getConstructorMethod(irConstructor: IrFunction): MethodHandle? {
            val intrinsicValue = irConstructor.parentAsClass.getEvaluateIntrinsicValue()
            if (intrinsicValue == "kotlin.Char" || intrinsicValue == "kotlin.Long") return null

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

        fun getStaticGetter(field: IrField): MethodHandle? {
            val jvmClass = field.parentAsClass.defaultType.getClass(true)
            val returnType = field.type.let { it.getClass((it as IrSimpleType).hasQuestionMark) }
            return MethodHandles.lookup().findStaticGetter(jvmClass, field.name.asString(), returnType)
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
            return if (this is IrSimpleFunction) {
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
            val owner = this.classOrNull?.owner
            val fqName = owner?.fqNameWhenAvailable?.asString()
            val notNullType = this.makeNotNull()
            //TODO check if primitive array is possible here
            return when {
                notNullType.isPrimitiveType() || notNullType.isString() -> getPrimitiveClass(notNullType, asObject)!!
                notNullType.isArray() -> {
                    val argumentFqName = (this as IrSimpleType).arguments.single().typeOrNull?.classOrNull?.owner?.fqNameWhenAvailable
                    argumentFqName?.let { Class.forName("[L$it;") } ?: Array<Any?>::class.java
                }
                notNullType.isNothing() -> Nothing::class.java
                notNullType.isAny() -> Any::class.java
                notNullType.isUnit() -> if (asObject) Void::class.javaObjectType else Void::class.javaPrimitiveType!!
                notNullType.isNumber() -> Number::class.java
                notNullType.isCharSequence() -> CharSequence::class.java
                notNullType.isComparable() -> Comparable::class.java
                notNullType.isThrowable() -> Throwable::class.java
                notNullType.isIterable() -> Iterable::class.java

                notNullType.isKFunction() -> Class.forName("kotlin.reflect.KFunction")
                notNullType.isFunction() -> {
                    val arity = fqName?.removePrefix("kotlin.Function")?.toIntOrNull()
                    return when {
                        arity == null || arity >= BuiltInFunctionArity.BIG_ARITY -> Class.forName("kotlin.jvm.functions.FunctionN")
                        else -> Class.forName("kotlin.jvm.functions.${fqName.removePrefix("kotlin.")}")
                    }
                }
                //notNullType.isSuspendFunction() || notNullType.isKSuspendFunction() -> throw AssertionError() //TODO

                fqName == "kotlin.Enum" -> Enum::class.java
                fqName == "kotlin.collections.Collection" || fqName == "kotlin.collections.MutableCollection" -> Collection::class.java
                fqName == "kotlin.collections.List" || fqName == "kotlin.collections.MutableList" -> List::class.java
                fqName == "kotlin.collections.Set" || fqName == "kotlin.collections.MutableSet" -> Set::class.java
                fqName == "kotlin.collections.Map" || fqName == "kotlin.collections.MutableMap" -> Map::class.java
                fqName == "kotlin.collections.ListIterator" || fqName == "kotlin.collections.MutableListIterator" -> ListIterator::class.java
                fqName == "kotlin.collections.Iterator" || fqName == "kotlin.collections.MutableIterator" -> Iterator::class.java
                fqName == "kotlin.collections.Map.Entry" || fqName == "kotlin.collections.MutableMap.MutableEntry" -> Map.Entry::class.java
                fqName == "kotlin.collections.ListIterator" || fqName == "kotlin.collections.MutableListIterator" -> ListIterator::class.java
                fqName == "kotlin.collections.HashMap" -> HashMap::class.java

                owner.hasAnnotation(evaluateIntrinsicAnnotation) -> Class.forName(owner!!.getEvaluateIntrinsicValue())
                fqName == null -> Any::class.java // null if this.isTypeParameter()
                else -> Class.forName(owner.internalName())
            }
        }

        private fun IrFunction.getOriginalOverriddenSymbols(): MutableList<IrFunctionSymbol> {
            val overriddenSymbols = mutableListOf<IrFunctionSymbol>()
            if (this is IrSimpleFunction) {
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
}
