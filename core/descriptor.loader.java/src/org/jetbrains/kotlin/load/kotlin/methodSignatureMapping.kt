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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.KotlinType

fun FunctionDescriptor.computeJvmDescriptor()
        = StringBuilder().apply {
            append(if (this@computeJvmDescriptor is ConstructorDescriptor) "<init>" else name.asString())
            append("(")

            valueParameters.forEach {
                appendErasedType(it.type)
            }

            append(")")

            if (hasVoidReturnType(this@computeJvmDescriptor)) {
                append("V")
            }
            else {
                appendErasedType(returnType!!)
            }
        }.toString()


val ClassDescriptor.internalName: String
    get() {
        return computeInternalName(this)
    }

private fun StringBuilder.appendErasedType(type: KotlinType) {
    append(
            JvmTypeFactoryImpl.toString(
                    mapType(type, JvmTypeFactoryImpl, TypeMappingMode.DEFAULT, TypeMappingConfigurationImpl, descriptorTypeWriter = null)))
}

private sealed class JvmType {
    // null means 'void'
    class Primitive(val jvmPrimitiveType: JvmPrimitiveType?) : JvmType()
    class Object(val internalName: String) : JvmType()
    class Array(val elementType: JvmType) : JvmType()
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

private object TypeMappingConfigurationImpl : TypeMappingConfiguration<JvmType> {
    override fun commonSupertype(types: Collection<KotlinType>): KotlinType {
        throw AssertionError("There should be no intersection type in existing descriptors, but found: " + types.joinToString())
    }

    override fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor) = null

    override fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor) {
        // DO nothing
    }
}
