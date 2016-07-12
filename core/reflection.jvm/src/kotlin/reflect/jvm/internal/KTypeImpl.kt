/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.load.java.structure.reflect.createArrayType
import org.jetbrains.kotlin.load.java.structure.reflect.primitiveByWrapper
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

internal class KTypeImpl(
        val type: KotlinType,
        computeJavaType: () -> Type
) : KType {
    internal val javaType: Type by ReflectProperties.lazySoft(computeJavaType)

    override val classifier: KClassifier?
        get() = convert(type)

    private fun convert(type: KotlinType): KClassifier? {
        val descriptor = type.constructor.declarationDescriptor
        when (descriptor) {
            is ClassDescriptor -> {
                val jClass = descriptor.toJavaClass() ?: return null
                if (jClass.isArray) {
                    // There may be no argument if it's a primitive array (such as IntArray)
                    val argument = type.arguments.singleOrNull()?.type ?: return KClassImpl(jClass)

                    val elementClassifier = convert(argument)
                    val elementType = when (elementClassifier) {
                        is KClass<*> -> elementClassifier
                        is KTypeParameter -> {
                            // For arrays of type parameters (`Array<T>`) we return the KClass representing `Array<Any>`
                            // since there's no other sensible option
                            // TODO: return `Array<erasure-of-T>`
                            Any::class
                        }
                        else -> TODO("Arrays of type alias classifiers are not yet supported")
                    }
                    return KClassImpl(elementType.java.createArrayType())
                }

                if (!TypeUtils.isNullableType(type)) {
                    return KClassImpl(jClass.primitiveByWrapper ?: jClass)
                }

                return KClassImpl(jClass)
            }
            is TypeParameterDescriptor -> return KTypeParameterImpl(descriptor)
            is TypeAliasDescriptor -> TODO("Type alias classifiers are not yet supported")
            else -> return null
        }
    }

    override val isMarkedNullable: Boolean
        get() = type.isMarkedNullable

    override fun equals(other: Any?) =
            other is KTypeImpl && type == other.type

    override fun hashCode() =
            type.hashCode()

    override fun toString() =
            ReflectionObjectRenderer.renderType(type)
}
