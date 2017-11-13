/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.load.java.isFromJavaOrBuiltins
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.KotlinType

fun FunctionDescriptor.computeJvmDescriptor(withReturnType: Boolean = true)
        = StringBuilder().apply {
            append(if (this@computeJvmDescriptor is ConstructorDescriptor) "<init>" else name.asString())
            append("(")

            valueParameters.forEach {
                appendErasedType(it.type)
            }

            append(")")

            if (withReturnType) {
                if (hasVoidReturnType(this@computeJvmDescriptor)) {
                    append("V")
                }
                else {
                    appendErasedType(returnType!!)
                }
            }
        }.toString()

// Boxing is only necessary for 'remove(E): Boolean' of a MutableCollection<Int> implementation
// Otherwise this method might clash with 'remove(I): E' defined in the java.util.List JDK interface (mapped to kotlin 'removeAt')
fun forceSingleValueParameterBoxing(f: CallableDescriptor): Boolean {
    if (f !is FunctionDescriptor) return false

    if (f.valueParameters.size != 1 || f.isFromJavaOrBuiltins() || f.name.asString() != "remove") return false
    if ((f.original.valueParameters.single().type.mapToJvmType() as? JvmType.Primitive)?.jvmPrimitiveType != JvmPrimitiveType.INT) return false

    val overridden =
            BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(f)
            ?: return false

    val overriddenParameterType = overridden.original.valueParameters.single().type.mapToJvmType()
    return overridden.containingDeclaration.fqNameUnsafe == KotlinBuiltIns.FQ_NAMES.mutableCollection.toUnsafe()
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
            return JvmClassName.byClassId(it).internalName
        }

        return computeInternalName(this)
    }

internal val ClassId.internalName: String
    get() {
        return JvmClassName.byClassId(JavaToKotlinClassMap.mapKotlinToJava(asSingleFqName().toUnsafe()) ?: this).internalName
    }

private fun StringBuilder.appendErasedType(type: KotlinType) {
    append(type.mapToJvmType())
}

internal fun KotlinType.mapToJvmType() =
        mapType(this, JvmTypeFactoryImpl, TypeMappingMode.DEFAULT, TypeMappingConfigurationImpl, descriptorTypeWriter = null)

sealed class JvmType {
    // null means 'void'
    class Primitive(val jvmPrimitiveType: JvmPrimitiveType?) : JvmType()
    class Object(val internalName: String) : JvmType()
    class Array(val elementType: JvmType) : JvmType()

    override fun toString() = JvmTypeFactoryImpl.toString(this)
}

private object JvmTypeFactoryImpl : JvmTypeFactory<JvmType> {
    override fun boxType(possiblyPrimitiveType: JvmType) =
            when {
                possiblyPrimitiveType is JvmType.Primitive && possiblyPrimitiveType.jvmPrimitiveType != null ->
                    createObjectType(
                            JvmClassName.byFqNameWithoutInnerClasses(
                                    possiblyPrimitiveType.jvmPrimitiveType.wrapperFqName).internalName)
                else -> possiblyPrimitiveType
            }

    override fun createFromString(representation: String): JvmType {
        assert(representation.length > 0) { "empty string as JvmType" }
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

    override fun createObjectType(internalName: String) = JvmType.Object(internalName)

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

    override fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor) = null
    override fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String? = null

    override fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor) {
        // DO nothing
    }
}
