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

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.lang.reflect.Array

val Class<*>.safeClassLoader: ClassLoader
    get() = classLoader ?: ClassLoader.getSystemClassLoader()

fun Class<*>.isEnumClassOrSpecializedEnumEntryClass(): Boolean =
        Enum::class.java.isAssignableFrom(this)

private val WRAPPER_TO_PRIMITIVE = listOf(
       Boolean::class, Byte::class, Char::class, Double::class, Float::class, Int::class, Long::class, Short::class
).map { it.javaObjectType to it.javaPrimitiveType }.toMap()

val Class<*>.primitiveByWrapper: Class<*>?
    get() = WRAPPER_TO_PRIMITIVE[this]

/**
 * NOTE: does not perform a Java -> Kotlin mapping. If this is not expected, consider using KClassImpl#classId instead
 */
val Class<*>.classId: ClassId
    get() = when {
        isPrimitive -> throw IllegalArgumentException("Can't compute ClassId for primitive type: $this")
        isArray -> throw IllegalArgumentException("Can't compute ClassId for array type: $this")
        enclosingMethod != null || enclosingConstructor != null || simpleName.isEmpty() -> {
            val fqName = FqName(name)
            ClassId(fqName.parent(), FqName.topLevel(fqName.shortName()), /* local = */ true)
        }
        else -> declaringClass?.classId?.createNestedClassId(Name.identifier(simpleName)) ?: ClassId.topLevel(FqName(name))
    }

val Class<*>.desc: String
    get() {
        if (this == Void.TYPE) return "V"
        // This is a clever exploitation of a format returned by Class.getName(): for arrays, it's almost an internal name,
        // but with '.' instead of '/'
        return createArrayType().name.substring(1).replace('.', '/')
    }

fun Class<*>.createArrayType(): Class<*> =
        Array.newInstance(this, 0).javaClass
