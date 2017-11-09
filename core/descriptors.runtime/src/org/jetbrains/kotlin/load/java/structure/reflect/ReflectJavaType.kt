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

package org.jetbrains.kotlin.load.java.structure.reflect

import org.jetbrains.kotlin.load.java.structure.JavaType
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

abstract class ReflectJavaType : JavaType {
    protected abstract val reflectType: Type

    companion object Factory {
        fun create(type: Type): ReflectJavaType {
            return when {
                type is Class<*> && type.isPrimitive -> ReflectJavaPrimitiveType(type)
                type is GenericArrayType || type is Class<*> && type.isArray -> ReflectJavaArrayType(type)
                type is WildcardType -> ReflectJavaWildcardType(type)
                else -> ReflectJavaClassifierType(type)
            }
        }
    }

    override fun equals(other: Any?) = other is ReflectJavaType && reflectType == other.reflectType

    override fun hashCode() = reflectType.hashCode()

    override fun toString() = this::class.java.name + ": " + reflectType
}
