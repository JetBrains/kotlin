/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.load.java.isFromJavaOrBuiltins
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.KotlinType

fun FunctionDescriptor.computeJvmDescriptor(withReturnType: Boolean = true, withName: Boolean = true): String = buildString {
    if (withName) {
        append(if (this@computeJvmDescriptor is ConstructorDescriptor) "<init>" else name.asString())
    }

    append("(")

    extensionReceiverParameter?.let {
        appendErasedType(it.type)
    }

    for (parameter in valueParameters) {
        appendErasedType(parameter.type)
    }

    append(")")

    if (withReturnType) {
        if (hasVoidReturnType(this@computeJvmDescriptor)) {
            append("V")
        } else {
            appendErasedType(returnType!!)
        }
    }
}

fun PropertyDescriptor.computeJvmDescriptorWithoutName() = buildString {
    append("(")

    extensionReceiverParameter?.let {
        appendErasedType(it.type)
    }

    append(")")

    if (hasVoidReturnType(this@computeJvmDescriptorWithoutName)) {
        append("V")
    } else {
        appendErasedType(returnType!!)
    }
}

// Boxing is only necessary for 'remove(E): Boolean' of a MutableCollection<Int> implementation
// Otherwise this method might clash with 'remove(I): E' defined in the java.util.List JDK interface (mapped to kotlin 'removeAt')
fun forceSingleValueParameterBoxing(f: CallableDescriptor): Boolean {
    if (f !is FunctionDescriptor) return false

    if (f.name.asString() != "remove" || f.valueParameters.size != 1 || f.isFromJavaOrBuiltins()) return false
    if ((f.original.valueParameters.single().type.mapToJvmType() as? JvmType.Primitive)?.jvmPrimitiveType != JvmPrimitiveType.INT) return false

    val overridden =
        BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(f)
            ?: return false

    val overriddenParameterType = overridden.original.valueParameters.single().type.mapToJvmType()
    return overridden.containingDeclaration.fqNameUnsafe == StandardNames.FqNames.mutableCollection.toUnsafe()
            && overriddenParameterType is JvmType.Object && overriddenParameterType.internalName == "java/lang/Object"
}

// This method only returns not-null for class methods
internal fun CallableDescriptor.computeJvmSignature(): String? = signatures {
    if (DescriptorUtils.isLocal(this@computeJvmSignature)) return null

    val classDescriptor = containingDeclaration as? ClassDescriptor ?: return null
    if (classDescriptor.name.isSpecial) return null

    signature(
        classDescriptor,
        (original as? SimpleFunctionDescriptor ?: return null).computeJvmDescriptor()
    )
}

internal val ClassDescriptor.internalName: String
    get() {
        JavaToKotlinClassMap.mapKotlinToJava(fqNameSafe.toUnsafe())?.let {
            return JvmClassName.internalNameByClassId(it)
        }

        return computeInternalName(this)
    }

private fun StringBuilder.appendErasedType(type: KotlinType) {
    append(type.mapToJvmType())
}

internal fun KotlinType.mapToJvmType(): JvmType =
    mapType(this, JvmTypeFactoryImpl, TypeMappingMode.DEFAULT, TypeMappingConfigurationImpl, descriptorTypeWriter = null)

sealed class JvmType {
    // null means 'void'
    class Primitive(val jvmPrimitiveType: JvmPrimitiveType?) : JvmType()

    class Object(val internalName: String) : JvmType()
    class Array(val elementType: JvmType) : JvmType()

    override fun toString() = JvmTypeFactoryImpl.toString(this)

    companion object {
        internal val BOOLEAN = Primitive(JvmPrimitiveType.BOOLEAN)
        internal val CHAR = Primitive(JvmPrimitiveType.CHAR)
        internal val BYTE = Primitive(JvmPrimitiveType.BYTE)
        internal val SHORT = Primitive(JvmPrimitiveType.SHORT)
        internal val INT = Primitive(JvmPrimitiveType.INT)
        internal val FLOAT = Primitive(JvmPrimitiveType.FLOAT)
        internal val LONG = Primitive(JvmPrimitiveType.LONG)
        internal val DOUBLE = Primitive(JvmPrimitiveType.DOUBLE)
    }
}

private object JvmTypeFactoryImpl : JvmTypeFactory<JvmType> {
    override fun boxType(possiblyPrimitiveType: JvmType) =
        when {
            possiblyPrimitiveType is JvmType.Primitive && possiblyPrimitiveType.jvmPrimitiveType != null ->
                createObjectType(
                    JvmClassName.byFqNameWithoutInnerClasses(possiblyPrimitiveType.jvmPrimitiveType.wrapperFqName).internalName
                )
            else -> possiblyPrimitiveType
        }

    override fun createFromString(representation: String): JvmType {
        assert(representation.isNotEmpty()) { "empty string as JvmType" }
        val firstChar = representation[0]

        JvmPrimitiveType.values().firstOrNull { it.desc[0] == firstChar }?.let {
            return JvmType.Primitive(it)
        }

        return when (firstChar) {
            'V' -> JvmType.Primitive(null)
            '[' -> JvmType.Array(createFromString(representation.substring(1)))
            else -> {
                assert(firstChar == 'L' && representation.endsWith(';')) {
                    "Type that is not primitive nor array should be Object, but '$representation' was found"
                }

                JvmType.Object(representation.substring(1, representation.length - 1))
            }
        }
    }

    override fun createPrimitiveType(primitiveType: PrimitiveType): JvmType =
        when (primitiveType) {
            PrimitiveType.BOOLEAN -> JvmType.BOOLEAN
            PrimitiveType.CHAR -> JvmType.CHAR
            PrimitiveType.BYTE -> JvmType.BYTE
            PrimitiveType.SHORT -> JvmType.SHORT
            PrimitiveType.INT -> JvmType.INT
            PrimitiveType.FLOAT -> JvmType.FLOAT
            PrimitiveType.LONG -> JvmType.LONG
            PrimitiveType.DOUBLE -> JvmType.DOUBLE
        }

    override fun createObjectType(internalName: String): JvmType.Object =
        JvmType.Object(internalName)

    override fun toString(type: JvmType): String =
        when (type) {
            is JvmType.Array -> "[" + toString(type.elementType)
            is JvmType.Primitive -> type.jvmPrimitiveType?.desc ?: "V"
            is JvmType.Object -> "L" + type.internalName + ";"
        }

    override val javaLangClassType: JvmType
        get() = createObjectType("java/lang/Class")

}

internal object TypeMappingConfigurationImpl : TypeMappingConfiguration<JvmType> {
    override fun commonSupertype(types: Collection<KotlinType>): KotlinType {
        throw AssertionError("There should be no intersection type in existing descriptors, but found: " + types.joinToString())
    }

    override fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor): JvmType? = null
    override fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String? = null

    override fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor) {
        // DO nothing
    }
}
