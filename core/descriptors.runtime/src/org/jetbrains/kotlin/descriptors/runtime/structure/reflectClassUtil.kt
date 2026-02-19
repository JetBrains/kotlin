/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.runtime.structure

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.lang.reflect.Array
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

val Class<*>.safeClassLoader: ClassLoader
    get() = classLoader ?: ClassLoader.getSystemClassLoader()

fun Class<*>.isEnumClassOrSpecializedEnumEntryClass(): Boolean =
    Enum::class.java.isAssignableFrom(this)

private val PRIMITIVE_CLASSES =
    listOf(Boolean::class, Byte::class, Char::class, Double::class, Float::class, Int::class, Long::class, Short::class)
private val WRAPPER_TO_PRIMITIVE = PRIMITIVE_CLASSES.map { it.javaObjectType to it.javaPrimitiveType }.toMap()
private val PRIMITIVE_TO_WRAPPER = PRIMITIVE_CLASSES.map { it.javaPrimitiveType to it.javaObjectType }.toMap()

val Class<*>.primitiveByWrapper: Class<*>?
    get() = WRAPPER_TO_PRIMITIVE[this]

val Class<*>.wrapperByPrimitive: Class<*>?
    get() = PRIMITIVE_TO_WRAPPER[this]

private val FUNCTION_CLASSES =
    listOf(
        Function0::class.java, Function1::class.java, Function2::class.java, Function3::class.java, Function4::class.java,
        Function5::class.java, Function6::class.java, Function7::class.java, Function8::class.java, Function9::class.java,
        Function10::class.java, Function11::class.java, Function12::class.java, Function13::class.java, Function14::class.java,
        Function15::class.java, Function16::class.java, Function17::class.java, Function18::class.java, Function19::class.java,
        Function20::class.java, Function21::class.java, Function22::class.java
    ).mapIndexed { i, clazz -> clazz to i }.toMap()

val Class<*>.functionClassArity: Int?
    get() = FUNCTION_CLASSES[this]

/**
 * NOTE: does not perform a Java -> Kotlin mapping. If this is not expected, consider using KClassImpl#classId instead
 */
val Class<*>.classId: ClassId
    get() = when {
        isPrimitive -> throw IllegalArgumentException("Can't compute ClassId for primitive type: $this")
        isArray -> throw IllegalArgumentException("Can't compute ClassId for array type: $this")
        enclosingMethod != null || enclosingConstructor != null || simpleName.isEmpty() -> {
            val fqName = FqName(name)
            ClassId(fqName.parent(), FqName.topLevel(fqName.shortName()), isLocal = true)
        }
        else -> declaringClass?.classId?.createNestedClassId(Name.identifier(simpleName)) ?: ClassId.topLevel(FqName(name))
    }

val Class<*>.desc: String
    get() = when {
        isPrimitive -> when (name) {
            "boolean" -> "Z"
            "char" -> "C"
            "byte" -> "B"
            "short" -> "S"
            "int" -> "I"
            "float" -> "F"
            "long" -> "J"
            "double" -> "D"
            "void" -> "V"
            else -> throw IllegalArgumentException("Unsupported primitive type: $this")
        }
        isArray -> name.replace('.', '/')
        else -> "L${name.replace('.', '/')};"
    }

/**
 * @return all arguments of a parameterized type, including those of outer classes in case this type represents an inner generic.
 * The returned list starts with the arguments to the innermost class, then continues with those of its outer class, and so on.
 * For example, for the type `Outer<A, B>.Inner<C, D>` the result would be `[C, D, A, B]`.
 */
val Type.parameterizedTypeArguments: List<Type>
    get() {
        if (this !is ParameterizedType) return emptyList()
        if (ownerType == null) return actualTypeArguments.toList()

        return generateSequence(this) { it.ownerType as? ParameterizedType }.flatMap { it.actualTypeArguments.asSequence() }.toList()
    }
